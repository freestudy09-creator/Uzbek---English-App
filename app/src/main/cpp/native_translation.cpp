#include <jni.h>
#include <android/log.h>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>
#include <cctype>

#include <ctranslate2/translator.h>
#include <sentencepiece_processor.h>

#define LOG_TAG "TilMateAI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
std::mutex g_mutex;
std::unique_ptr<ctranslate2::Translator> g_translator;
std::unique_ptr<sentencepiece::SentencePieceProcessor> g_sp;
std::string g_model_path;
std::string g_spm_path;

std::string jstringToUtf8(JNIEnv* env, jstring value) {
    if (!value) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string out(chars ? chars : "");
    if (chars) env->ReleaseStringUTFChars(value, chars);
    return out;
}

jstring makeJavaString(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}


std::vector<std::string> splitForTranslation(const std::string& text) {
    std::vector<std::string> parts;
    std::string current;
    current.reserve(text.size());

    auto flush = [&]() {
        size_t start = current.find_first_not_of(" \t\r\n");
        size_t end = current.find_last_not_of(" \t\r\n");
        if (start != std::string::npos && end != std::string::npos) {
            parts.push_back(current.substr(start, end - start + 1));
        }
        current.clear();
    };

    // Preserve the context of ordinary long sentences. Split at genuine
    // sentence boundaries first. Only force a split for exceptionally long
    // text, and then prefer whitespace rather than commas/semicolons.
    for (char ch : text) {
        current.push_back(ch);
        if (ch == '.' || ch == '!' || ch == '?' || ch == '\n') {
            flush();
        } else if (current.size() >= 700 && std::isspace(static_cast<unsigned char>(ch))) {
            flush();
        }
    }
    flush();

    if (parts.empty() && !text.empty()) parts.push_back(text);
    return parts;
}

void ensureEngine(const std::string& modelPath, const std::string& spmPath) {
    if (g_translator && g_sp && g_model_path == modelPath && g_spm_path == spmPath) {
        return;
    }

    g_translator.reset();
    g_sp.reset();

    auto sp = std::make_unique<sentencepiece::SentencePieceProcessor>();
    auto status = sp->Load(spmPath);
    if (!status.ok()) {
        throw std::runtime_error("Could not load SentencePiece model: " + status.ToString());
    }

    auto translator = std::make_unique<ctranslate2::Translator>(
        modelPath,
        ctranslate2::Device::CPU
    );

    g_sp = std::move(sp);
    g_translator = std::move(translator);
    g_model_path = modelPath;
    g_spm_path = spmPath;
}
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_uzeng_languagebridge_MainActivity_nativeTranslate(
    JNIEnv* env,
    jclass,
    jstring modelPathJ,
    jstring spmPathJ,
    jstring textJ) {

    try {
        const std::string modelPath = jstringToUtf8(env, modelPathJ);
        const std::string spmPath = jstringToUtf8(env, spmPathJ);
        std::string text = jstringToUtf8(env, textJ);

        if (text.size() >= 2) {
            const bool asciiQuotes =
                (text.front() == '"' && text.back() == '"') ||
                (text.front() == '\'' && text.back() == '\'');
            const std::string openCurly = u8"“";
            const std::string closeCurly = u8"”";
            const bool curlyQuotes =
                text.rfind(openCurly, 0) == 0 &&
                text.size() >= openCurly.size() + closeCurly.size() &&
                text.compare(text.size() - closeCurly.size(), closeCurly.size(), closeCurly) == 0;

            if (asciiQuotes) {
                text = text.substr(1, text.size() - 2);
            } else if (curlyQuotes) {
                text = text.substr(openCurly.size(),
                    text.size() - openCurly.size() - closeCurly.size());
            }
        }

        if (modelPath.empty() || spmPath.empty() || text.empty()) {
            return makeJavaString(env, "");
        }

        std::lock_guard<std::mutex> lock(g_mutex);
        ensureEngine(modelPath, spmPath);

        const auto segments = splitForTranslation(text);
        std::vector<std::vector<std::string>> batch;
        batch.reserve(segments.size());

        for (const auto& segment : segments) {
            std::vector<std::string> tokens;
            auto encodeStatus = g_sp->Encode(segment, &tokens);
            if (!encodeStatus.ok()) {
                throw std::runtime_error("Tokenization failed: " + encodeStatus.ToString());
            }
            batch.push_back(std::move(tokens));
        }

        ctranslate2::TranslationOptions options;
        options.beam_size = 4;
        options.max_decoding_length = 256;

        const auto results = g_translator->translate_batch(batch, options);
        if (results.size() != batch.size()) {
            throw std::runtime_error("Translation returned incomplete result");
        }

        std::string fullOutput;
        for (size_t i = 0; i < results.size(); ++i) {
            if (results[i].hypotheses.empty()) {
                throw std::runtime_error("Translation returned no result");
            }

            std::string decoded;
            auto decodeStatus = g_sp->Decode(results[i].hypotheses[0], &decoded);
            if (!decodeStatus.ok()) {
                throw std::runtime_error("Detokenization failed: " + decodeStatus.ToString());
            }

            if (!fullOutput.empty()) fullOutput += " ";
            fullOutput += decoded;
        }

        return makeJavaString(env, fullOutput);
    } catch (const std::exception& e) {
        LOGE("Native translation error: %s", e.what());
        jclass ex = env->FindClass("java/lang/RuntimeException");
        if (ex) env->ThrowNew(ex, e.what());
        return nullptr;
    }
}
