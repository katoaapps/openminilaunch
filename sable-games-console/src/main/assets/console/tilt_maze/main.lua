-- Tilt Maze for Sable Games Console.
-- The first map is rustmix-wave's (MIT, Piyush Daiya); the rest are new. S is the start, G the goal.
local LEVELS = {
  "#########/#S..#...#/#.#.#.#.#/#.#...#.#/#.###.#.#/#...#.#.#/###.#...#/#.....#G#/#########",
  "#########/#S#.....#/#.#.###.#/#...#...#/#####.#.#/#.....#.#/#.#####.#/#......G#/#########",
  "###########/#S....#...#/####.##.#.#/#....#..#.#/#.####.##.#/#......#..#/######.#.##/#....#.#..#/#.##...##.#/#..#.#...G#/###########",
  "###########/#S#.......#/#.#.#####.#/#.#.#...#.#/#...#.#.#.#/#####.#.#.#/#.....#.#.#/#.#####.#.#/#.#.....#.#/#...#####G#/###########",
  "#############/#S..#.......#/##.##.#####.#/#...#.#...#.#/#.###.#.#.#.#/#...#...#.#.#/###.#####.#.#/#.#.......#.#/#.#######.#.#/#.......#.#.#/#.#####.#.#.#/#.....#...#G#/#############",
  "#############/#S......#...#/#.#####.#.#.#/#.#...#...#.#/#.#.#.#####.#/#...#.#.....#/#####.#.#####/#.....#.....#/#.#########.#/#.#.......#.#/#.#.#####.#.#/#...#......G#/#############",
}

local REPEAT = 0.18
local level, map, px, py, gx, gy, moves, clock, lean, wait

local function cell(x, y)
  local row = map[y + 1]
  if not row or x < 0 or x >= #row then return "#" end
  return row:sub(x + 1, x + 1)
end

local function start_level()
  map = {}
  for row in LEVELS[level]:gmatch("[^/]+") do map[#map + 1] = row end
  for y = 0, #map - 1 do
    for x = 0, #map[1] - 1 do
      local c = cell(x, y)
      if c == "S" then px, py = x, y end
      if c == "G" then gx, gy = x, y end
    end
  end
  sable.grid(#map[1], #map)
  moves, clock, lean, wait = 0, 0, nil, 0
end

local function report()
  sable.status(string.format("Level %d of %d · %d moves · %d:%02d", level, #LEVELS, moves,
    math.floor(clock / 60), math.floor(clock % 60)))
end

local function move(dx, dy)
  if cell(px + dx, py + dy) == "#" then return end
  px, py = px + dx, py + dy
  moves = moves + 1
  if px == gx and py == gy then
    sable.score(level)
    if level == #LEVELS then
      sable.save("level", "1")
      sable.over("Every maze done! Space plays again.")
    else
      level = level + 1
      sable.save("level", tostring(level))
      start_level()
    end
  end
end

local DIRS = { left = { -1, 0 }, right = { 1, 0 }, up = { 0, -1 }, down = { 0, 1 } }

function init()
  level = tonumber(sable.load("level") or "1") or 1
  if level < 1 or level > #LEVELS then level = 1 end
  start_level()
  report()
end

function tilt(step)
  local d = DIRS[step]
  if d then move(d[1], d[2]); wait = REPEAT * 2 end
end

function key(name)
  if name == "space" then start_level() end
end

-- Holding the phone tilted keeps the ball rolling, one cell at a time.
function update(dt)
  clock = clock + dt
  local x, y = sable.tilt()
  if math.abs(x) > 0.6 or math.abs(y) > 0.6 then
    wait = wait - dt
    if wait <= 0 then
      if math.abs(x) >= math.abs(y) then move(x < 0 and -1 or 1, 0) else move(0, y < 0 and -1 or 1) end
      wait = REPEAT
    end
  else
    wait = 0
  end
  report()
end

function draw()
  for y = 0, #map - 1 do
    for x = 0, #map[1] - 1 do
      local c = cell(x, y)
      if c == "#" then sable.rect(x, y, 1, 1, "wall")
      elseif c == "G" then sable.rect(x, y, 1, 1, "goal")
      else sable.rect(x, y, 1, 1, "floor") end
    end
  end
  sable.circle(px + 0.5, py + 0.5, 0.36, "player")
end
