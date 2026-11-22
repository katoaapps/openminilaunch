-- Sokoban Tilt for Sable Games Console: one tilt (or arrow key) is one step; walk into a crate to
-- push it. The first level is rustmix-wave's (MIT, Piyush Daiya); the rest are new.
-- # wall, . floor, S you, C crate, G mark, * crate on a mark.
local LEVELS = {
  "#########/#S......#/#.###...#/#.......#/#...C...#/#.......#/#...G...#/#.......#/#########",
  "########/#S.....#/#..C.C.#/#.##...#/#G..#..#/#..G...#/########",
  "########/##..S###/#..C..##/#.#.#.G#/#.C..#.#/#G...C.#/###.G..#/########",
  "#########/#...#...#/#.C...C.#/#..#S#..#/#.G...G.#/#...C...#/#..G#...#/#########",
  "########/#..#...#/#.CC.G.#/#S.#.G.#/##.C.G.#/#......#/########",
}

local level, walls, goals, crates, px, py, w, h, moves, history

local function id(x, y) return y * 64 + x end

local function start_level()
  walls, goals, crates, history, moves = {}, {}, {}, {}, 0
  local rows = {}
  for row in LEVELS[level]:gmatch("[^/]+") do rows[#rows + 1] = row end
  h, w = #rows, #rows[1]
  for y = 0, h - 1 do
    for x = 0, w - 1 do
      local c = rows[y + 1]:sub(x + 1, x + 1)
      if c == "#" then walls[id(x, y)] = true end
      if c == "G" or c == "*" then goals[id(x, y)] = true end
      if c == "C" or c == "*" then crates[id(x, y)] = true end
      if c == "S" then px, py = x, y end
    end
  end
  sable.grid(w, h)
end

local function solved()
  for k in pairs(crates) do if not goals[k] then return false end end
  return true
end

local function report()
  sable.status(string.format("Level %d of %d · %d moves · U undoes", level, #LEVELS, moves))
end

local function copy(t)
  local out = {}
  for k, v in pairs(t) do out[k] = v end
  return out
end

local function step(dx, dy)
  local nx, ny = px + dx, py + dy
  if nx < 0 or ny < 0 or nx >= w or ny >= h or walls[id(nx, ny)] then return end
  local saved = { px = px, py = py, crates = copy(crates) }
  if crates[id(nx, ny)] then
    local bx, by = nx + dx, ny + dy
    if bx < 0 or by < 0 or bx >= w or by >= h or walls[id(bx, by)] or crates[id(bx, by)] then return end
    crates[id(nx, ny)] = nil
    crates[id(bx, by)] = true
  end
  history[#history + 1] = saved
  px, py, moves = nx, ny, moves + 1
  if solved() then
    sable.score(level)
    if level == #LEVELS then
      sable.save("level", "1")
      sable.over(string.format("All %d levels solved! Space plays again.", #LEVELS))
      return
    end
    level = level + 1
    sable.save("level", tostring(level))
    start_level()
  end
  report()
end

local DIRS = { left = { -1, 0 }, right = { 1, 0 }, up = { 0, -1 }, down = { 0, 1 } }

function init()
  level = tonumber(sable.load("level") or "1") or 1
  if level < 1 or level > #LEVELS then level = 1 end
  start_level()
  report()
end

function tilt(dir)
  local d = DIRS[dir]
  if d then step(d[1], d[2]) end
end

function key(name)
  if name == "u" and #history > 0 then
    local last = table.remove(history)
    px, py, crates, moves = last.px, last.py, last.crates, moves - 1
    report()
  elseif name == "space" then
    start_level()
    report()
  end
end

function draw()
  for y = 0, h - 1 do
    for x = 0, w - 1 do
      local k = id(x, y)
      sable.rect(x, y, 1, 1, walls[k] and "wall" or "floor")
      if goals[k] then sable.circle(x + 0.5, y + 0.5, 0.18, "goal") end
      if crates[k] then
        sable.rect(x + 0.12, y + 0.12, 0.76, 0.76, goals[k] and "good" or "box")
      end
    end
  end
  sable.circle(px + 0.5, py + 0.5, 0.34, "player")
end
