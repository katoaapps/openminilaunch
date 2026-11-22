-- Tilt Circuit for Sable Games Console: three laps of a closed circuit against two rival cars.
-- Lean the phone to steer, lean it back to brake; the grass slows you down. C changes your car.
-- Two circuits: an easy oval for starters and a twisty one. The tracks are TRBRY's "Track Tiles"
-- (OpenGameArt, CC-BY 3.0), their pieces laid out into these circuits and cut into 10-cell
-- squares; the cars are EdSquare's "Top Down F1 Cars" (CC0).
local VIEW = 12
local TILE = 10
local EASY = {
  { 7.408, 3.875, 12.408, 3.875 },
  { 12.408, 3.875, 17.408, 3.875 },
  { 17.408, 3.875, 20.942, 7.408, 17.408, 7.408, 1 },
  { 20.942, 7.408, 20.942, 12.408 },
  { 20.942, 12.408, 17.408, 15.942, 17.408, 12.408, 1 },
  { 17.408, 15.942, 12.408, 15.942 },
  { 12.408, 15.942, 7.408, 15.942 },
  { 7.408, 15.942, 3.875, 12.408, 7.408, 12.408, 1 },
  { 3.875, 12.408, 3.875, 7.408 },
  { 3.875, 7.408, 7.408, 3.875, 7.408, 7.408, 1 },
}
local TWISTY = {
  { 8.658, 12.192, 13.658, 12.192 },
  { 13.658, 12.192, 17.192, 8.658, 13.658, 8.658, -1 },
  { 17.192, 8.658, 20.725, 5.125, 20.725, 8.658, 1 },
  { 20.725, 5.125, 25.725, 5.125 },
  { 25.725, 5.125, 29.258, 8.658, 25.725, 8.658, 1 },
  { 29.258, 8.658, 32.791, 12.192, 32.791, 8.658, -1 },
  { 32.791, 12.192, 37.791, 12.192 },
  { 37.791, 12.192, 41.325, 15.725, 37.791, 15.725, 1 },
  { 41.325, 15.725, 41.325, 20.725 },
  { 41.325, 20.725, 41.325, 25.725 },
  { 41.325, 25.725, 37.791, 29.258, 37.791, 25.725, 1 },
  { 37.791, 29.258, 32.791, 29.258 },
  { 32.791, 29.258, 29.258, 25.725, 32.791, 25.725, 1 },
  { 29.258, 25.725, 25.725, 22.192, 25.725, 25.725, -1 },
  { 25.725, 22.192, 20.725, 22.192 },
  { 20.725, 22.192, 17.192, 25.725, 20.725, 25.725, -1 },
  { 17.192, 25.725, 13.658, 29.258, 13.658, 25.725, 1 },
  { 13.658, 29.258, 8.658, 29.258 },
  { 8.658, 29.258, 5.125, 25.725, 8.658, 25.725, 1 },
  { 5.125, 25.725, 5.125, 20.725 },
  { 5.125, 20.725, 5.125, 15.725 },
  { 5.125, 15.725, 8.658, 12.192, 8.658, 15.725, 1 },
}
-- Each circuit: its pieces, its squares (columns, rows, picture names), the piece the start line
-- is on, and how fast the rivals go there.
local CIRCUITS = {
  { name = "Easy oval", pieces = EASY, tiles = { 3, 2, "easy" }, start = 1, pace = 0.65 },
  { name = "Twisty", pieces = TWISTY, tiles = { 5, 4, "track" }, start = 10, pace = 1 },
}
local HALF_ROAD, KERB = 2.5, 0.45
local CAR_W, CAR_H = 1.6, 2.0
local LAPS = 3
local TOP, ACCEL, BRAKE, GRASS_TOP, STEER = 10, 6, 14, 3.5, 3.4
local COLORS = { "red", "blue", "yellow", "green", "purple", "violet" }
local RIVALS = { { pace = 8.4, lane = 1.4 }, { pace = 8.9, lane = -1.4 } }

-- Each piece: its length, where it starts along the lap, and for a bend its centre, radius,
-- starting angle and sweep (clockwise is positive, as y grows downwards).
for _, c in ipairs(CIRCUITS) do
  c.length = 0
  for _, p in ipairs(c.pieces) do
    if p[5] then
      p.r = math.sqrt((p[1] - p[5]) ^ 2 + (p[2] - p[6]) ^ 2)
      p.a0 = math.atan2(p[2] - p[6], p[1] - p[5])
      p.sweep = p[7] * math.pi / 2
      p.len = p.r * math.pi / 2
    else
      p.len = math.sqrt((p[3] - p[1]) ^ 2 + (p[4] - p[2]) ^ 2)
    end
    p.at = c.length
    c.length = c.length + p.len
  end
  c.tile_list = {}
  for row = 0, c.tiles[2] - 1 do
    for col = 0, c.tiles[1] - 1 do
      c.tile_list[#c.tile_list + 1] = { col * TILE, row * TILE, string.format("%s_%d_%d.png", c.tiles[3], row, col) }
    end
  end
end

-- The circuit being raced: its pieces, the length of a lap and where the start line is (two
-- cells into a straight).
local circuit, TRACK, LENGTH, START_D
local function use(n)
  circuit = CIRCUITS[n]
  TRACK, LENGTH = circuit.pieces, circuit.length
  START_D = TRACK[circuit.start].at + 2
end
use(2)

-- The point t (0 to 1) along a piece, and the direction of travel there.
local function along(p, t)
  if p[5] then
    local a = p.a0 + p.sweep * t
    local s = p[7]
    return p[5] + p.r * math.cos(a), p[6] + p.r * math.sin(a), -math.sin(a) * s, math.cos(a) * s
  end
  local dx, dy = (p[3] - p[1]) / p.len, (p[4] - p[2]) / p.len
  return p[1] + (p[3] - p[1]) * t, p[2] + (p[4] - p[2]) * t, dx, dy
end

-- The place d along the lap: x, y and the direction of travel.
local function at_distance(d)
  d = d % LENGTH
  for _, p in ipairs(TRACK) do
    if d <= p.at + p.len then return along(p, (d - p.at) / p.len) end
  end
  return along(TRACK[1], 0)
end

-- How far (x, y) is from the middle of the road, and how far along the lap that nearest point is.
local function nearest(x, y)
  local best, where = math.huge, 0
  for _, p in ipairs(TRACK) do
    local t
    if p[5] then
      local d = math.atan2(y - p[6], x - p[5]) - p.a0
      d = (d + math.pi) % (2 * math.pi) - math.pi
      t = d / p.sweep
    else
      t = ((x - p[1]) * (p[3] - p[1]) + (y - p[2]) * (p[4] - p[2])) / (p.len * p.len)
    end
    t = math.max(0, math.min(1, t))
    local px, py = along(p, t)
    local dist = math.sqrt((x - px) ^ 2 + (y - py) ^ 2)
    if dist < best then best, where = dist, p.at + p.len * t end
  end
  return best, where
end

local car, rivals, clock, countdown, lap_start, best_lap, wrong, finished
local mine = tonumber(sable.load("color") or "1") or 1
if not COLORS[mine] then mine = 1 end
-- The circuit menu shows before each race; the choice is kept.
local menu = true
local choice = tonumber(sable.load("circuit") or "1") or 1
if not CIRCUITS[choice] then choice = 1 end

local function other(n)
  local i = (mine + n - 1) % #COLORS + 1
  return i
end

local function place()
  local ahead = 1
  for _, r in ipairs(rivals) do
    if r.d > car.d then ahead = ahead + 1 end
  end
  return ahead
end

local ORDINAL = { "1st", "2nd", "3rd" }

local function report()
  if menu then
    sable.status("Choose a circuit · Up/Down picks · Space starts · C colour")
    return
  end
  local lap = math.max(1, math.min(LAPS, math.floor(car.d / LENGTH) + 1))
  local text
  if countdown > 0 then
    text = "Get ready · C changes colour"
  else
    text = string.format("Lap %d/%d · %.1f s · %s", lap, LAPS, clock, ORDINAL[place()])
    if wrong > 0.7 then text = text .. " · Wrong way"
    elseif car.off then text = text .. " · Off track"
    elseif best_lap then text = text .. string.format(" · best lap %.1f s", best_lap) end
  end
  sable.status(text)
end

local function start_race()
  menu = false
  use(choice)
  sable.save("circuit", tostring(choice))
  local sx, sy, dx, dy = at_distance(START_D - 2.2)
  car = { x = sx, y = sy, a = math.atan2(dy, dx), v = 0, d = -2.2, last = LENGTH - 2.2, lap = -1, sector = 3,
    off = false }
  rivals = {}
  for i, r in ipairs(RIVALS) do
    rivals[i] = { d = -0.2 - (i - 1) * 4.2, v = 0, pace = r.pace * circuit.pace, lane = r.lane, color = i }
  end
  clock, countdown, lap_start, best_lap, wrong, finished = 0, 3, 0, nil, 0, false
  report()
end

function init()
  sable.grid(VIEW, VIEW)
  menu = true
  use(choice)
  local sx, sy, dx, dy = at_distance(START_D - 2.2)
  car = { x = sx, y = sy, a = math.atan2(dy, dx), v = 0, d = -2.2, last = LENGTH - 2.2, lap = -1, sector = 3,
    off = false }
  rivals, clock, countdown, wrong, finished = {}, 0, 3, 0, false
  report()
end

function tilt(step)
  if not menu then return end
  if step == "up" or step == "left" then choice = math.max(1, choice - 1) end
  if step == "down" or step == "right" then choice = math.min(#CIRCUITS, choice + 1) end
  report()
end

local function drive_rivals(dt)
  for _, r in ipairs(rivals) do
    local x, y, dx, dy = at_distance(START_D + r.d)
    local _, _, nx, ny = at_distance(START_D + r.d + 3)
    -- Slower where the road bends ahead.
    local bend = 1 - (dx * nx + dy * ny)
    local want = r.pace * (1 - 0.35 * math.min(1, bend * 2))
    r.v = r.v + math.max(-BRAKE * dt, math.min(ACCEL * 0.8 * dt, want - r.v))
    r.d = r.d + r.v * dt
  end
end

local function rival_place(r)
  local x, y, dx, dy = at_distance(START_D + r.d)
  return x - dy * r.lane, y + dx * r.lane, dx, dy
end

function update(dt)
  if finished or menu then return end
  dt = math.min(dt, 0.1)
  if countdown > 0 then
    countdown = countdown - dt
    report()
    return
  end
  clock = clock + dt
  local lean, back = sable.tilt()
  car.a = car.a + lean * STEER * dt * math.min(1, car.v / 3)
  local top = car.off and GRASS_TOP or TOP
  if back > 0.35 then
    car.v = math.max(0, car.v - BRAKE * dt)
  elseif car.v > top then
    car.v = math.max(top, car.v - BRAKE * 0.6 * dt)
  else
    car.v = math.min(top, car.v + ACCEL * dt)
  end
  car.x = car.x + math.cos(car.a) * car.v * dt
  car.y = car.y + math.sin(car.a) * car.v * dt
  drive_rivals(dt)
  -- Touching a rival (a box a little smaller than the car) pushes you off to the side and slows you.
  for _, r in ipairs(rivals) do
    local rx, ry, dx, dy = rival_place(r)
    local gx, gy = car.x - rx, car.y - ry
    local ahead, across = gx * dx + gy * dy, gy * dx - gx * dy
    if math.abs(ahead) < CAR_H * 0.85 and math.abs(across) < CAR_W * 0.8 then
      local push = (across < 0 and -1 or 1) * (CAR_W * 0.8 - math.abs(across))
      car.x, car.y = car.x - dy * push, car.y + dx * push
      car.v = car.v * 0.8
    end
  end
  local dist, where = nearest(car.x, car.y)
  where = (where - START_D) % LENGTH
  car.off = dist > HALF_ROAD + KERB
  local step = where - car.last
  if step > LENGTH / 2 then step = step - LENGTH elseif step < -LENGTH / 2 then step = step + LENGTH end
  car.last = where
  wrong = (step < -0.001 and car.v > 1) and wrong + dt or math.max(0, wrong - dt)
  -- The lap is split into four sectors that count only in order, so a lap needs the whole circuit.
  local sector = math.floor(where / (LENGTH / 4))
  if sector == (car.sector + 1) % 4 then
    car.sector = sector
    if sector == 0 then
      car.lap = car.lap + 1
      if car.lap >= 1 then
        local t = clock - lap_start
        if not best_lap or t < best_lap then best_lap = t end
      end
      lap_start = clock
    end
  end
  if sector == car.sector then car.d = car.lap * LENGTH + where end
  if car.lap >= LAPS then
    finished = true
    sable.score(math.floor(clock + 0.5))
    local key = "best_" .. choice
    local old = tonumber(sable.load(key) or "")
    if not old or clock < old then sable.save(key, string.format("%.1f", clock)) end
    sable.over(string.format("%s in %.1f s · best lap %.1f s · Space races again",
      ORDINAL[place()], clock, best_lap or clock))
  end
  report()
end

function key(name)
  if menu and (name == "space" or name == "enter") then
    start_race()
  elseif name == "c" then
    mine = mine % #COLORS + 1
    sable.save("color", tostring(mine))
    report()
  end
end

-- The menu: each circuit's shape, drawn small from points along its middle.
for _, c in ipairs(CIRCUITS) do
  c.outline = {}
  local d, pieces = 0, c.pieces
  for k = 0, 47 do
    d = c.length * k / 48
    for _, p in ipairs(pieces) do
      if d <= p.at + p.len then
        local x, y = along(p, (d - p.at) / p.len)
        c.outline[#c.outline + 1] = { x, y }
        break
      end
    end
  end
end

local function draw_menu()
  sable.rect(0, 0, VIEW, VIEW, "#2F5B3A")
  sable.text(VIEW / 2, 1.1, "Tilt Circuit", "#FFFFFF", 1.1)
  for n, c in ipairs(CIRCUITS) do
    local top = 2.3 + (n - 1) * 4.9
    local on = n == choice
    sable.rect(0.6, top, VIEW - 1.2, 4.4, on and "#FFFFFF33" or "#00000033")
    if on then sable.rect(0.6, top, 0.2, 4.4, "#FFE066") end
    -- The shape, fitted into a 4 by 3.4 box on the left.
    local w, h = c.tiles[1] * TILE, c.tiles[2] * TILE
    local k = math.min(3.6 / w, 3.4 / h)
    for _, pt in ipairs(c.outline) do
      sable.circle(1.2 + (pt[1] * k) + (3.6 - w * k) / 2, top + 0.5 + pt[2] * k + (3.4 - h * k) / 2, 0.16, "#3C3C3C")
    end
    local best = sable.load("best_" .. n)
    sable.text(8.1, top + 1.4, c.name, on and "#FFE066" or "#FFFFFF", 0.7)
    sable.text(8.1, top + 2.6, best and ("best " .. best .. " s") or (n == 1 and "for starters" or "tight bends"),
      "#DDDDDD", 0.5)
  end
end

local function sprite(x, y, a, color, ox, oy)
  local deg = math.deg(a) + 90
  sable.image(x - ox - CAR_W / 2, y - oy - CAR_H / 2, CAR_W, CAR_H, "f1_" .. COLORS[color] .. ".png", deg)
end

function draw()
  if menu then
    draw_menu()
    return
  end
  -- The camera looks a little ahead of the car.
  local ox = car.x + math.cos(car.a) * 1.5 - VIEW / 2
  local oy = car.y + math.sin(car.a) * 1.5 - VIEW / 2
  sable.rect(0, 0, VIEW, VIEW, "#2F5B3A")
  for _, t in ipairs(circuit.tile_list) do
    if t[1] < ox + VIEW and t[1] + TILE > ox and t[2] < oy + VIEW and t[2] + TILE > oy then
      sable.image(t[1] - ox, t[2] - oy, TILE, TILE, t[3])
    end
  end
  -- The start line: a chequered band across the road.
  local sx, sy, dx, dy = at_distance(START_D)
  for i = 0, 9 do
    for j = 0, 1 do
      local c = (i + j) % 2 == 0 and "#F2F2F2" or "#1E1E1E"
      local across, ahead = i * 0.5 - HALF_ROAD + 0.25, j * 0.5 - 0.25
      local cx, cy = sx - dy * across + dx * ahead, sy + dx * across + dy * ahead
      sable.rect(cx - 0.25 - ox, cy - 0.25 - oy, 0.5, 0.5, c)
    end
  end
  for i, r in ipairs(rivals) do
    local x, y, dx, dy = rival_place(r)
    sprite(x, y, math.atan2(dy, dx), other(i), ox, oy)
  end
  sprite(car.x, car.y, car.a, mine, ox, oy)
  -- The countdown sits on the road ahead of the car.
  local tx, ty = car.x + math.cos(car.a) * 4 - ox, car.y + math.sin(car.a) * 4 - oy
  if countdown > 0 then
    sable.text(tx, ty, tostring(math.ceil(countdown)), "#FFE066", 3)
  elseif clock < 0.8 then
    sable.text(tx, ty, "Go!", "#7CD992", 3)
  end
end
