-- Ramp Rush for Sable Games Console: a stunt car seen from behind, racing down a road through the
-- sunset. Lean to steer, lean back to brake, Space to boost. Ramps throw the car over gaps; in the
-- air, leaning rolls the car, and each full roll you land takes a second off your time. Land on
-- your wheels or start again from the last flag. Three tracks; C changes the car's colour.
local VIEW = 12
local HORIZON = 4.6
local CAM_BACK, CAM_UP = 3.2, 2.1
local DRAW = 110             -- segments drawn ahead
local ROAD = 2.0             -- half the road's width, in world units
local TOP, ACCEL, BRAKE = 30, 11, 30
local BOOST_TOP, BOOST_TIME = 44, 1.6
local GRAVITY = 26
local ROLL_RATE = 7.5
local COLORS = { "blue", "red", "green", "orange" }

-- A track is a list of sections: { segments, curve, rise, kind }. Curve bends the road (per
-- segment), rise is the height gained over the section, and kind is road, ramp, gap, boost or check.
local TRACKS = {
  {
    name = "Sunset Hop",
    { 40, 0, 0, "road" }, { 50, 0.02, 0, "road" }, { 30, 0, 4, "road" }, { 30, 0, -4, "road" },
    { 8, 0, 0, "check" }, { 12, 0, 3, "ramp" }, { 14, 0, 0, "gap" }, { 40, 0, -3, "road" },
    { 40, -0.025, 0, "road" }, { 10, 0, 0, "boost" }, { 40, 0, 0, "road" }, { 8, 0, 0, "check" },
    { 12, 0, 3.5, "ramp" }, { 18, 0, 0, "gap" }, { 40, 0, -3.5, "road" }, { 50, 0.03, 2, "road" },
    { 50, -0.03, -2, "road" }, { 40, 0, 0, "road" },
  },
  {
    name = "Canyon Line",
    { 40, 0, 0, "road" }, { 10, 0, 0, "boost" }, { 30, 0, 0, "road" }, { 12, 0, 3, "ramp" },
    { 16, 0, 0, "gap" }, { 30, 0, -3, "road" }, { 8, 0, 0, "check" }, { 60, 0.035, 5, "road" },
    { 60, -0.035, -5, "road" }, { 10, 0, 0, "boost" }, { 20, 0, 0, "road" }, { 14, 0, 4.5, "ramp" },
    { 24, 0, 0, "gap" }, { 40, 0, -4.5, "road" }, { 8, 0, 0, "check" }, { 40, 0.02, 0, "road" },
    { 12, 0, 3, "ramp" }, { 16, 0, 0, "gap" }, { 30, 0, -3, "road" }, { 40, 0, 0, "road" },
  },
  {
    name = "Sky Jumps",
    { 30, 0, 0, "road" }, { 10, 0, 0, "boost" }, { 14, 0, 5, "ramp" }, { 26, 0, 0, "gap" },
    { 30, 0, -5, "road" }, { 8, 0, 0, "check" }, { 50, -0.04, 3, "road" }, { 50, 0.04, -3, "road" },
    { 10, 0, 0, "boost" }, { 14, 0, 5, "ramp" }, { 28, 0, 0, "gap" }, { 30, 0, -5, "road" },
    { 8, 0, 0, "check" }, { 40, 0.03, 0, "road" }, { 10, 0, 0, "boost" }, { 16, 0, 6, "ramp" },
    { 30, 0, 0, "gap" }, { 40, 0, -6, "road" }, { 40, 0, 0, "road" },
  },
}

-- Each built segment: x and y of its near edge's middle, its curve, kind, and stripe.
local segs, finish_z
local function build(track)
  segs = {}
  local x, dx, y = 0, 0, 0
  for _, s in ipairs(track) do
    local n, curve, rise, kind = s[1], s[2], s[3], s[4]
    for i = 1, n do
      -- Hills ease in and out; ramps rise in a straight line so they launch the car.
      local t0, t1 = (i - 1) / n, i / n
      local e0 = kind == "ramp" and t0 or (1 - math.cos(math.pi * t0)) / 2
      local e1 = kind == "ramp" and t1 or (1 - math.cos(math.pi * t1)) / 2
      segs[#segs + 1] = { x = x, y = y, kind = kind, first = i == 1, dy = rise * (e1 - e0) }
      y = y + rise * (e1 - e0)
      dx = dx + curve
      x = x + dx
    end
  end
  segs[#segs + 1] = { x = x, y = y, kind = "road", dy = 0 }
  finish_z = #segs - 30
end

local function seg(z)
  local i = math.floor(z)
  if i < 0 then i = 0 end
  return segs[math.min(#segs, i + 1)], segs[math.min(#segs, i + 2)], z - i
end

-- The road's middle x and height at z, or nil height over a gap.
local function road_at(z)
  local a, b, f = seg(z)
  return a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f, a.kind
end

local track, car, clock, check_z, rolls_total, boost, fuel, message, message_t, finished, color, total
color = tonumber(sable.load("color") or "1") or 1
if not COLORS[color] then color = 1 end

local function place_at(z)
  local _, ry = road_at(z)
  car = { z = z, x = 0, v = 0, h = ry, vh = 0, air = false, roll = 0, spun = 0, steer = 0 }
end

local function start_track(n)
  track = n
  build(TRACKS[n])
  check_z = 2
  place_at(check_z)
  clock, boost, fuel = 0, 0, 1
  message, message_t = TRACKS[n].name, 2
end

local function report()
  if finished then return end
  local bar = string.rep("▮", math.floor(fuel * 4 + 0.5)) .. string.rep("▯", 4 - math.floor(fuel * 4 + 0.5))
  sable.status(string.format("Track %d/%d · %.1f s · %d rolls · boost %s", track, #TRACKS, clock, rolls_total, bar))
end

function init()
  sable.grid(VIEW, VIEW)
  rolls_total, finished, total = 0, false, 0
  start_track(1)
  report()
end

function key(name)
  if name == "space" and fuel >= 0.25 and boost <= 0 and not finished then
    boost = BOOST_TIME * fuel
    fuel = 0
  elseif name == "c" then
    color = color % #COLORS + 1
    sable.save("color", tostring(color))
  end
end

local function say(text)
  message, message_t = text, 1.3
end

local function crash(text)
  say(text)
  clock = clock + 1
  place_at(check_z)
end

function update(dt)
  if finished then return end
  dt = math.min(dt, 0.05)
  clock = clock + dt
  message_t = math.max(0, message_t - dt)
  local lean, back = sable.tilt()
  local steps = 3
  local h = dt / steps
  for _ = 1, steps do
    local _, ry, kind = road_at(car.z)
    local top = boost > 0 and BOOST_TOP or TOP
    if not car.air and math.abs(car.x) > 1.1 then top = top * 0.45 end
    if back > 0.35 and not car.air then
      car.v = math.max(0, car.v - BRAKE * h)
    elseif car.v > top then
      car.v = math.max(top, car.v - BRAKE * 0.5 * h)
    else
      car.v = math.min(top, car.v + (boost > 0 and ACCEL * 2.5 or ACCEL) * h)
    end
    if kind == "boost" and not car.air then
      boost = math.max(boost, 0.8)
    end
    boost = math.max(0, boost - h)
    local zn = car.z + car.v * h
    if car.air then
      car.vh = car.vh - GRAVITY * h
      car.h = car.h + car.vh * h
      car.roll = car.roll + lean * ROLL_RATE * h
      car.spun = car.spun + lean * ROLL_RATE * h
      car.z = zn
      local _, gy, gk = road_at(car.z)
      if gk == "gap" then
        if car.h < gy - 6 then crash("Into the canyon!") return end
      elseif car.h <= gy then
        -- Landing: wheels down (roll near a whole turn) or a crash.
        local turns = car.roll / (2 * math.pi)
        local off = math.abs(turns - math.floor(turns + 0.5)) * 2 * math.pi
        if off > 0.75 then crash("Crash!") return end
        local done = math.floor(math.abs(car.spun) / (2 * math.pi) + 0.25)
        if done > 0 then
          rolls_total = rolls_total + done
          clock = clock - done
          fuel = math.min(1, fuel + 0.5 * done)
          say(done == 1 and "Barrel roll! -1 s" or string.format("%d rolls! -%d s", done, done))
        end
        car.air, car.h, car.vh, car.roll, car.spun = false, gy, 0, 0, 0
      end
    else
      -- On the road: steer, and the curve pulls the car outwards.
      local a = seg(car.z)
      local nexts = seg(car.z + 1)
      local curve = nexts.x - a.x - (a.x - (segs[math.max(1, math.floor(car.z))] or a).x)
      car.steer = lean
      car.x = car.x + (lean * 2.4 - curve * car.v * 0.9) * h * math.min(1, car.v / 10)
      car.x = math.max(-1.8, math.min(1.8, car.x))
      local _, ny, nk = road_at(zn)
      local slope = (ny - ry) / math.max(1e-6, zn - car.z)
      car.z = zn
      if nk == "gap" or ny < ry - 0.001 and kind == "ramp" then
        -- Off the end of a ramp (or the road drops away): take off at the slope we had.
        car.air = true
        car.vh = math.max(0, car.prev_slope or slope) * car.v
        car.h = ry
      else
        car.h = ny
        car.prev_slope = slope
      end
    end
  end
  local _, _, kind = road_at(car.z)
  local cs = segs[math.floor(car.z) + 1]
  if kind == "check" and cs and cs.first and car.z - 1 > check_z then
    check_z = math.floor(car.z)
    say("Checkpoint")
  end
  if car.z >= finish_z then
    total = total + clock
    if track == #TRACKS then
      finished = true
      sable.score(math.floor(total + 0.5))
      sable.over(string.format("Finished in %.1f s with %d rolls · Space races again", total, rolls_total))
      return
    end
    start_track(track + 1)
  end
  report()
end

-- Drawing ---------------------------------------------------------------------------------------

-- Colours fade into the evening haze with distance; each colour gets 16 steps, worked out once.
local HAZE = { 0xF7, 0xA8, 0x77 }
local FADE = 16
local BASE = {
  sand = "#D9A066", sand2 = "#CC9258", road = "#4A4A52", road2 = "#53535C", line = "#F2F2F2",
  red = "#C0392B", white = "#E8E8E8", ramp = "#F2C230", ramp2 = "#2A2A2A", boost = "#3FD0E6",
  boost2 = "#2A9FB3", gap = "#4A2A2A", gap2 = "#42252A",
}
local SHADE = {}
for name, hex in pairs(BASE) do
  local r, g, b = tonumber(hex:sub(2, 3), 16), tonumber(hex:sub(4, 5), 16), tonumber(hex:sub(6, 7), 16)
  SHADE[name] = {}
  for k = 0, FADE - 1 do
    local f = k / (FADE - 1) * 0.9
    SHADE[name][k] = string.format("#%02X%02X%02X", math.floor(r + (HAZE[1] - r) * f + 0.5),
      math.floor(g + (HAZE[2] - g) * f + 0.5), math.floor(b + (HAZE[3] - b) * f + 0.5))
  end
end

-- A band across the whole view, with square corners.
local CANYON = 9    -- how far below the road a gap's floor lies

local function band(y, h, color)
  sable.poly({ 0, y, VIEW, y, VIEW, y + h, 0, y + h }, color)
end

local function quad(fx, fy, fw, nx, ny, nw, color)
  sable.poly({ fx - fw, fy, fx + fw, fy, nx + nw, ny, nx - nw, ny }, color)
end

-- Roadside things: every so many segments, a cactus or a boulder on one side.
local function scenery(p, base_x)
  local i = p.i
  if i % 7 ~= 0 or p.s.kind == "gap" then return end
  local side = (i % 14 == 0) and 1 or -1
  local far_out = 3.2 + (i * 37 % 5) * 0.6
  local sx = VIEW / 2 + (p.s.x + side * far_out - base_x) * p.sc * VIEW / 2
  local unit = p.sc * VIEW / 2
  if (i * 13) % 3 == 0 then
    local w, h = 1.2 * unit, 0.75 * unit
    sable.image(sx - w / 2, p.y - h * 0.95, w, h, "boulder.png")
  else
    local w, h = 0.8 * unit, 1.6 * unit
    sable.image(sx - w / 2, p.y - h * 0.97, w, h, "cactus.png")
  end
end

function draw()
  local a = seg(car.z)
  local turn = a.x * 0.08
  sable.image(0, 0, VIEW, HORIZON + 0.2, "sky.png")
  for _, m in ipairs({ { "mesa_far.png", 14, 3.5, 1 }, { "mesa_near.png", 16, 3.2, 2 } }) do
    local mx = -((turn * m[4] * 2) % m[2])
    sable.image(mx, HORIZON - m[3] + 0.15, m[2], m[3], m[1])
    sable.image(mx + m[2], HORIZON - m[3] + 0.15, m[2], m[3], m[1])
  end
  band(HORIZON, VIEW - HORIZON, SHADE.sand[FADE - 1])

  local cam_z = car.z - CAM_BACK
  local cx, cy = road_at(math.max(0, cam_z))
  local base_x = cx + car.x * ROAD * 0.75
  local base_y = math.max(cy, car.h - 1.2) + CAM_UP
  -- Project every segment edge from the camera, then paint far to near.
  local first = math.max(0, math.floor(cam_z) + 1)
  local proj = {}
  for i = first, math.min(#segs - 1, first + DRAW) do
    local s = segs[i + 1]
    local dz = i - cam_z
    if dz > 0.4 then
      local sc = 1 / dz
      proj[#proj + 1] = {
        i = i, s = s, sc = sc,
        x = VIEW / 2 + (s.x - base_x) * sc * VIEW / 2,
        y = HORIZON - (s.y - (s.kind == "gap" and CANYON or 0) - base_y) * sc * VIEW / 2,
        w = ROAD * sc * VIEW / 2,
        fade = math.min(FADE - 1, math.floor((dz / DRAW) ^ 0.8 * FADE)),
      }
    end
  end
  for k = #proj, 2, -1 do
    local far, near = proj[k], proj[k - 1]
    if near.y > far.y and far.y < VIEW then
      local s, f = near.s, near.fade
      local dark = math.floor(near.i / 3) % 2 == 0
      local h = near.y - far.y + 0.03
      if s.kind == "gap" then
        if far.s.kind ~= "gap" then
          -- The far cliff of a canyon, below the road you land on.
          sable.image(0, far.y, VIEW, h, "cliff.png")
        else
          band(far.y, h, SHADE[dark and "gap" or "gap2"][f])
        end
      else
        band(far.y, h, SHADE[dark and "sand" or "sand2"][f])
        quad(far.x, far.y, far.w * 1.14, near.x, near.y, near.w * 1.14, SHADE[dark and "red" or "white"][f])
        local road = dark and "road" or "road2"
        if s.kind == "ramp" then road = dark and "ramp" or "ramp2" end
        if s.kind == "boost" then road = dark and "boost" or "boost2" end
        quad(far.x, far.y, far.w, near.x, near.y, near.w, SHADE[road][f])
        if s.kind == "road" and dark then
          quad(far.x, far.y, far.w * 0.035, near.x, near.y, near.w * 0.035, SHADE.line[f])
        elseif s.kind == "boost" and dark then
          local mx, my = (far.x + near.x) / 2, (far.y + near.y) / 2
          local w = near.w * 0.4
          sable.poly({ mx, far.y - h * 0.4, mx + w, near.y, mx, my + h * 0.2, mx - w, near.y }, "#FFFFFFAA")
        end
      end
      if far.s.kind == "gap" and s.kind ~= "gap" then
        -- The cliff edge where a gap ends: a lip of rock.
        band(far.y, math.max(0.04, h * 0.4), "#5A3426")
      end
      scenery(near, base_x)
      if s.kind == "check" and s.first then
        local w = near.w * 2.4
        sable.image(near.x - w / 2, near.y - w * 0.62, w, w * 0.625, "arch.png")
      end
    end
  end

  -- The car, its shadow on the road, and the boost flame.
  local _, ry = road_at(car.z)
  local lift = (car.h - ry) * 0.9
  local cw, ch = 3.0, 1.64
  local carx = VIEW / 2 + (car.x * ROAD * 0.25) * VIEW / 2 / CAM_BACK
  local cary = 9.2 - lift
  local shadow = math.max(0.35, 1 - lift / 6)
  local _, _, under = road_at(car.z)
  if under ~= "gap" then sable.image(carx - cw * 0.55 * shadow, 9.2 + ch * 0.32, cw * 1.1 * shadow, 0.55 * shadow, "shadow.png") end
  if boost > 0 then
    local f = 0.8 + (clock * 9 % 1) * 0.4
    sable.image(carx - 0.3, cary + ch * 0.2, 0.6, 1.2 * f, "flame.png")
  end
  local tiltdeg = car.air and math.deg(car.roll) or car.steer * 6
  sable.image(carx - cw / 2, cary - ch / 2, cw, ch, "car_" .. COLORS[color] .. ".png", tiltdeg)
  if message_t > 0 then
    band(1.75, 1.3, "#00000044")
    sable.text(VIEW / 2, 2.4, message, "#FFFFFF", 0.9)
  end
end
