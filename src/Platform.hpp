#pragma once

#include <SDL2/SDL_events.h>

namespace Platform
{
void Init();
void HandleEvent(const SDL_Event &event);
}; // namespace Platform
