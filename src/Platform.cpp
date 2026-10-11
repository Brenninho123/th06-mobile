#include "Platform.hpp"

#include <SDL2/SDL.h>

#include "FileSystem.hpp"
#include "GameWindow.hpp"
#include "Supervisor.hpp"
#include "TouchInput.hpp"
#include "i18n.hpp"

#ifdef __ANDROID__
#include <SDL2/SDL_system.h>
#include <unistd.h>
#endif

void Platform::Init()
{
#ifdef __ANDROID__
    SDL_SetHint(SDL_HINT_ANDROID_TRAP_BACK_BUTTON, "1");
    SDL_SetHint(SDL_HINT_ORIENTATIONS, "LandscapeLeft LandscapeRight");
    SDL_SetHint(SDL_HINT_TOUCH_MOUSE_EVENTS, "0");
    SDL_SetHint(SDL_HINT_MOUSE_TOUCH_EVENTS, "0");

    const char *dataPath = SDL_AndroidGetInternalStoragePath();
    if (dataPath != NULL)
    {
        if (chdir(dataPath) != 0)
        {
            SDL_Log("Failed to enter data directory %s", dataPath);
        }
    }
#endif
}

void Platform::HandleEvent(const SDL_Event &event)
{
    switch (event.type)
    {
    case SDL_WINDOWEVENT:
        if (event.window.event == SDL_WINDOWEVENT_SIZE_CHANGED || event.window.event == SDL_WINDOWEVENT_RESIZED)
        {
            GameWindow::RefreshDrawableSize();
        }
        break;
    case SDL_APP_WILLENTERBACKGROUND:
        TouchInput::SetButtons(0);
        FileSystem::WriteDataToFile(TH_CONFIG_FILE, &g_Supervisor.cfg, sizeof(g_Supervisor.cfg));
        break;
    default:
        break;
    }
}
