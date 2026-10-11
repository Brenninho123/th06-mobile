#include "TouchInput.hpp"

#include <atomic>

#ifdef __ANDROID__
#include <jni.h>
#endif

static std::atomic<u16> g_TouchButtons{0};

u16 TouchInput::GetButtons()
{
    return g_TouchButtons.load(std::memory_order_relaxed);
}

void TouchInput::SetButtons(u16 buttons)
{
    g_TouchButtons.store(buttons, std::memory_order_relaxed);
}

#ifdef __ANDROID__
extern "C" JNIEXPORT void JNICALL Java_com_brenninho_th06mobile_TouchControlsView_nativeSetButtons(JNIEnv *env,
                                                                                                 jclass clazz,
                                                                                                 jint buttons)
{
    (void)env;
    (void)clazz;
    TouchInput::SetButtons((u16)buttons);
}
#endif
