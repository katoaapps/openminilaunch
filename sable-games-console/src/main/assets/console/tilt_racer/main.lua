-- Tilt Racer for Sable Games Console: a top-down road. Lean the phone to steer, keep clear of the
-- traffic, and see how far you get; the road speeds up as you go. Space gives a short boost and C
-- changes the colour of your car. The cars are EdSquare's "Top Down F1 Cars" (OpenGameArt, CC0).
local W, H = 12, 12
local ROAD_LEFT, ROAD_RIGHT = 1.5, 10.5
local LANES = { 3, 6, 9 }
local CAR_W, CAR_H, CAR_Y = 1.6, 2.0, 9.4
local STEER = 9            -- cells a second at a full lean
local START_SPEED, TOP_SPEED, SPEED_UP = 6, 18, 0.12
local METRES_PER_CELL = 4
local COLORS = { "red", "blue", "yellow", "green", "purple", "violet" }

local x, speed, distance, traffic, spawn_in, stripes, boost, cooldown, crashed
local mine = tonumber(sable.load("color") or "1") or 1
if not COLORS[mine] then mine = 1 end

-- A traffic colour other than the player's.
local function other()
  local i = math.random(#COLORS - 1)
  if i >= mine then i = i + 1 end
  return i
end

local function report()
  local km = distance * METRES_PER_CELL / 1000
  local text = string.format("%.2f km · %d km/h", km, math.floor(speed * 12))
  if boost > 0 then text = text .. " · boost"
  elseif distance < 40 then text = text .. " · C changes colour"
  elseif cooldown <= 0 then text = text .. " · Space boosts" end
  sable.status(text)
  sable.score(math.floor(distance * METRES_PER_CELL))
end

function init()
  sable.grid(W, H)
  x, speed, distance, traffic = (W - CAR_W) / 2, START_SPEED, 0, {}
  spawn_in, stripes, boost, cooldown, crashed = 1.0, 0, 0, 0, false
  report()
end

local function spawn()
  local first = math.random(#LANES)
  traffic[#traffic + 1] = { lane = first, y = -CAR_H, color = other() }
  -- Now and then a second car, never in all three lanes, so there is always a way through.
  if speed > 9 and math.random() < 0.35 then
    local second = (first % #LANES) + 1
    traffic[#traffic + 1] = { lane = second, y = -CAR_H - 0.5, color = other() }
  end
end

-- The F1 cars are narrow in the middle, so the sides give a little more room than the ends.
local function hits(car)
  local cx = LANES[car.lane] - CAR_W / 2
  local mx, my = 0.25, 0.15
  return x + mx < cx + CAR_W - mx and x + CAR_W - mx > cx + mx and
    CAR_Y + my < car.y + CAR_H - my and CAR_Y + CAR_H - my > car.y + my
end

function update(dt)
  if crashed then return end
  dt = math.min(dt, 0.1)
  local lean = sable.tilt()
  x = math.max(ROAD_LEFT, math.min(ROAD_RIGHT - CAR_W, x + lean * STEER * dt))
  speed = math.min(TOP_SPEED, speed + SPEED_UP * dt)
  local now = speed * (boost > 0 and 1.5 or 1)
  boost = math.max(0, boost - dt)
  cooldown = math.max(0, cooldown - dt)
  distance = distance + now * dt
  stripes = (stripes + now * dt) % 2
  spawn_in = spawn_in - dt
  if spawn_in <= 0 then
    spawn()
    spawn_in = math.max(0.45, 2.2 - speed * 0.09) + math.random() * 0.4
  end
  local kept = {}
  for _, car in ipairs(traffic) do
    car.y = car.y + now * 0.55 * dt
    if hits(car) then
      crashed = true
      sable.over(string.format("Crash! %.2f km · Space drives again · C changes colour",
        distance * METRES_PER_CELL / 1000))
    end
    if car.y < H then kept[#kept + 1] = car end
  end
  traffic = kept
  report()
end

function key(name)
  if name == "space" and cooldown <= 0 and not crashed then
    boost, cooldown = 1.2, 4.0
  elseif name == "c" then
    mine = mine % #COLORS + 1
    sable.save("color", tostring(mine))
    for _, car in ipairs(traffic) do
      if car.color == mine then car.color = other() end
    end
    report()
  end
end

local function car(cx, cy, color)
  sable.image(cx, cy, CAR_W, CAR_H, "f1_" .. COLORS[color] .. ".png")
end

function draw()
  sable.rect(0, 0, W, H, "#2F5B3A")
  sable.rect(ROAD_LEFT - 0.2, 0, ROAD_RIGHT - ROAD_LEFT + 0.4, H, "#3A3A3A")
  sable.rect(ROAD_LEFT - 0.2, 0, 0.15, H, "#F2F2F2")
  sable.rect(ROAD_RIGHT + 0.05, 0, 0.15, H, "#F2F2F2")
  for _, divider in ipairs({ 4.5, 7.5 }) do
    for i = -1, H do
      sable.rect(divider - 0.06, i * 2 + stripes, 0.12, 1, "#BFBFBF")
    end
  end
  for _, c in ipairs(traffic) do car(LANES[c.lane] - CAR_W / 2, c.y, c.color) end
  if boost > 0 then
    -- Exhaust flames behind the rear wing.
    local flicker = (stripes * 7) % 1 * 0.2
    sable.rect(x + CAR_W / 2 - 0.32, CAR_Y + CAR_H - 0.05, 0.2, 0.45 + flicker, "#FF8A00")
    sable.rect(x + CAR_W / 2 + 0.12, CAR_Y + CAR_H - 0.05, 0.2, 0.45 + flicker, "#FF8A00")
    sable.rect(x + CAR_W / 2 - 0.28, CAR_Y + CAR_H - 0.05, 0.12, 0.25, "#FFE066")
    sable.rect(x + CAR_W / 2 + 0.16, CAR_Y + CAR_H - 0.05, 0.12, 0.25, "#FFE066")
  end
  car(x, CAR_Y, mine)
  if crashed then
    sable.circle(x + CAR_W / 2, CAR_Y + 0.2, 0.55, "warn")
    sable.circle(x + CAR_W / 2, CAR_Y + 0.2, 0.3, "#FFE066")
  end
end
