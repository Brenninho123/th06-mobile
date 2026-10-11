#pragma once

#include <SDL2/SDL_video.h>

#include "ZunResult.hpp"
#include "graphics/GfxInterface.hpp"
#include "inttypes.hpp"

// The internal resolution EoSD uses. 640x480. I can't think of any reason anyone sane
//   would want to change this
#define GAME_WINDOW_WIDTH (640)
#define GAME_WINDOW_HEIGHT (480)

#ifndef GAME_WINDOW_WIDTH_DEFAULT
#define GAME_WINDOW_WIDTH_DEFAULT (GAME_WINDOW_WIDTH)
#endif

#ifndef GAME_WINDOW_HEIGHT_DEFAULT
#define GAME_WINDOW_HEIGHT_DEFAULT (GAME_WINDOW_HEIGHT)
#endif

struct WindowMetrics
{
    i32 drawableWidth;
    i32 drawableHeight;
    i32 viewportWidth;
    i32 viewportHeight;
    i32 viewportOffX;
    i32 viewportOffY;

    void Update(i32 width, i32 height);
    bool IsLetterboxed() const
    {
        return this->viewportOffX != 0 || this->viewportOffY != 0;
    }
};

extern WindowMetrics g_WindowMetrics;

#define GAME_WINDOW_WIDTH_REAL (g_WindowMetrics.drawableWidth)
#define GAME_WINDOW_HEIGHT_REAL (g_WindowMetrics.drawableHeight)
#define VIEWPORT_WIDTH (g_WindowMetrics.viewportWidth)
#define VIEWPORT_HEIGHT (g_WindowMetrics.viewportHeight)
#define VIEWPORT_OFF_X (g_WindowMetrics.viewportOffX)
#define VIEWPORT_OFF_Y (g_WindowMetrics.viewportOffY)

#define WIDTH_RESOLUTION_SCALE (((f32)VIEWPORT_WIDTH) / GAME_WINDOW_WIDTH)
#define HEIGHT_RESOLUTION_SCALE (((f32)VIEWPORT_HEIGHT) / GAME_WINDOW_HEIGHT)

enum RenderResult
{
    RENDER_RESULT_KEEP_RUNNING,
    RENDER_RESULT_EXIT_SUCCESS,
    RENDER_RESULT_EXIT_ERROR,
};

struct GameWindow
{
    RenderResult Render();
    static void Present();

    static void CreateGameWindow();
    static ZunResult InitD3dRendering();
    static void InitD3dDevice();
    static void RefreshDrawableSize();

    i32 isAppClosing;
    i32 lastActiveAppValue;
    i32 isAppActive;
    u8 curFrame;
    i32 screenSaveActive;
    i32 lowPowerActive;
    i32 powerOffActive;
    u32 renderBackendIndex;
};

extern GameWindow g_GameWindow;
extern i32 g_TickCountToEffectiveFramerate;
extern double g_LastFrameTime;
extern GfxInterface *g_GfxBackend;