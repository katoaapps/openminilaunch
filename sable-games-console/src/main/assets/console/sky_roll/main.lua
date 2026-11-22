-- Sky Roll for Sable Games Console: roll a ball across platforms floating in the sky. Lean the phone
-- to roll it, pick up the gems and reach the goal before the time runs out; roll off an edge and
-- that ball is lost. Eight levels, each a little harder. R starts the level again.
local VIEW = 12
local R = 0.45          -- the ball's radius
local PULL = 14         -- cells a second, a second, at a full lean
local DRAG = 0.55
local TOP = 11
local BALLS = 4
local FALL_TIME = 0.8

-- A level: floors {x, y, w, h}; slopes {x, y, w, h, push x, push y}; rails {x, y, w, h} that block;
-- movers {x, y, w, h, dx, dy, seconds}, platforms that glide by dx, dy and back; gems {x, y};
-- the goal {x, y, w, h} and the start. `route` is the way through, for the test drive.
local LEVELS = {
  {
    name = "First steps", time = 45, start = { 0, 0 },
    floors = { { -2, -20, 4, 22 } },
    gems = { { 0, -6 }, { -1, -10 }, { 1, -14 } },
    goal = { -2, -20, 4, 2 },
    route = { { 0, -19 } },
  },
  {
    name = "Corners", time = 50, start = { 0, 0 },
    floors = { { -1.5, -10, 3, 12 }, { -1.5, -13, 12, 3 }, { 7.5, -25, 3, 12 } },
    gems = { { 0, -6 }, { 4.5, -11.5 }, { 9, -18 } },
    goal = { 7.5, -25, 3, 2 },
    route = { { 0, -11.5 }, { 9, -11.5 }, { 9, -24 } },
  },
  {
    name = "Bridge", time = 50, start = { 0, 0 },
    floors = { { -3, -6, 6, 8 }, { -0.7, -16, 1.4, 10.5 }, { -3, -24, 6, 8 } },
    gems = { { 0, -9 }, { 0, -13 }, { -2, -20 }, { 2, -20 } },
    goal = { -1.5, -24, 3, 2 },
    route = { { 0, -5 }, { 0, -17 }, { 0, -23 } },
  },
  {
    name = "Zigzag", time = 55, start = { 0, 0 },
    floors = {
      { -1, -6, 2, 8 }, { -1, -8, 8, 2 }, { 5, -14, 2, 8 }, { -3, -16, 10, 2 }, { -3, -24, 2, 8 },
    },
    gems = { { 3, -7 }, { 6, -11 }, { 2, -15 }, { -2, -20 } },
    goal = { -3, -24, 2, 2 },
    route = { { 0, -7 }, { 6, -7 }, { 6, -15 }, { -2, -15 }, { -2, -23 } },
  },
  {
    name = "Windy slopes", time = 55, start = { 0, 0 },
    floors = { { -2, -30, 4, 32 } },
    slopes = { { -2, -10, 4, 4, 5, 0 }, { -2, -18, 4, 4, -5, 0 }, { -2, -26, 4, 3, 6, 0 } },
    gems = { { -1.2, -8 }, { 1.2, -16 }, { -1.2, -24.5 } },
    goal = { -2, -30, 4, 2 },
    route = { { 0, -29 } },
  },
  {
    name = "Lifts", time = 60, start = { 0, 0 },
    floors = { { -3, -4, 6, 6 }, { -3, -20, 6, 6 }, { 13, -22, 6, 8 } },
    movers = { { -1.5, -6, 3, 2, 0, -8, 5 }, { 3, -18, 2, 2, 8, 0, 5 } },
    gems = { { 0, -10 }, { 0, -17 }, { 16, -18 } },
    goal = { 13, -22, 6, 2 },
  },
  {
    name = "Rail run", time = 60, start = { 0, 0 },
    floors = { { -6, -14, 12, 16 } },
    rails = { { -6, -4.2, 9, 0.4 }, { -3, -8.2, 9, 0.4 }, { -6, -12.2, 9, 0.4 } },
    gems = { { 4.5, -6 }, { -4.5, -10 }, { 4.5, -13 } },
    goal = { -2, -14, 4, 1.6 },
    route = { { 4.5, -2 }, { 4.5, -6 }, { -4.5, -6 }, { -4.5, -10 }, { 4.5, -10 }, { 4.5, -13 }, { 0, -13.2 } },
  },
  {
    name = "Tightrope", time = 70, start = { 0, 0 },
    floors = {
      { -1, -2, 2, 4 }, { -0.6, -8, 1.2, 6 }, { -0.6, -9.2, 8, 1.2 }, { 6.2, -18, 1.2, 9 },
      { -4, -19.2, 11.4, 1.2 }, { -4, -26, 1.2, 7 }, { -5, -29, 3.2, 3 },
    },
    slopes = { { 1, -19.2, 4, 1.2, 0, 2.5 } },
    gems = { { 3.4, -8.6 }, { 6.8, -13 }, { 2, -18.6 }, { -3.4, -23 } },
    goal = { -5, -29, 3.2, 1.6 },
    route = { { 0, -8.6 }, { 6.8, -8.6 }, { 6.8, -18.6 }, { -3.4, -18.6 }, { -3.4, -28 } },
  },
}

local level, lv, ball, clock, balls, score, got, falling, cleared, finished, t_all

-- Where a moving platform is now; it waits a moment at each end.
local function mover_place(m)
  local k = math.max(0, math.min(1, 0.5 - 0.65 * math.cos(2 * math.pi * t_all / m[7])))
  return m[1] + m[5] * k, m[2] + m[6] * k
end

local function inside(px, py, x, y, w, h, grace)
  return px >= x - grace and px <= x + w + grace and py >= y - grace and py <= y + h + grace
end

local function start_level(n)
  -- Gems already picked up on this level stay picked up after a lost ball.
  if n ~= level then got = {} end
  level = n
  lv = LEVELS[n]
  t_all = 0
  ball = { x = lv.start[1], y = lv.start[2], vx = 0, vy = 0, rx = 0, ry = 0, ride = nil }
  clock, falling, cleared = lv.time, nil, nil
end

local function report()
  if finished then return end
  local dots = string.rep("●", balls)
  sable.status(string.format("Level %d/%d · %d s · balls %s · %d pts", level, #LEVELS,
    math.max(0, math.ceil(clock)), dots, score))
end

function init()
  sable.grid(VIEW, VIEW)
  balls, score, finished, t_all = BALLS, 0, false, 0
  level = nil
  start_level(1)
  report()
end

function key(name)
  if name == "r" and not finished and not falling and not cleared then
    start_level(level)
    report()
  end
end

-- What the ball is standing on: "floor", a slope (its push), a mover, or nil over the sky.
local function ground(x, y)
  for _, m in ipairs(lv.movers or {}) do
    local mx, my = mover_place(m)
    if inside(x, y, mx, my, m[3], m[4], 0.05) then return "mover", m end
  end
  for _, s in ipairs(lv.slopes or {}) do
    if inside(x, y, s[1], s[2], s[3], s[4], 0) then return "slope", s end
  end
  for _, f in ipairs(lv.floors) do
    if inside(x, y, f[1], f[2], f[3], f[4], 0.05) then return "floor", f end
  end
  return nil
end

local function lose_ball(why)
  balls = balls - 1
  if balls <= 0 then
    finished = true
    sable.score(score)
    sable.over(string.format("%s on level %d · %d pts · Space plays again", why, level, score))
  else
    start_level(level)
  end
end

local function push_out(b, x, y, w, h)
  local cx = math.max(x, math.min(x + w, b.x))
  local cy = math.max(y, math.min(y + h, b.y))
  local dx, dy = b.x - cx, b.y - cy
  local d2 = dx * dx + dy * dy
  if d2 >= R * R then return end
  local d = math.sqrt(d2)
  if d < 1e-6 then dx, dy, d = 0, 1, 1 end
  local nx, ny = dx / d, dy / d
  b.x, b.y = b.x + nx * (R - d), b.y + ny * (R - d)
  local vn = b.vx * nx + b.vy * ny
  if vn < 0 then b.vx, b.vy = b.vx - 1.4 * vn * nx, b.vy - 1.4 * vn * ny end
end

function update(dt)
  if finished then return end
  dt = math.min(dt, 0.05)
  t_all = t_all + dt
  if cleared then
    cleared = cleared - dt
    if cleared <= 0 then
      if level == #LEVELS then
        finished = true
        sable.score(score)
        sable.over(string.format("All %d levels! %d pts · Space plays again", #LEVELS, score))
        return
      end
      start_level(level + 1)
    end
    report()
    return
  end
  if falling then
    falling = falling + dt
    ball.x, ball.y = ball.x + ball.vx * dt * 0.5, ball.y + ball.vy * dt * 0.5
    if falling >= FALL_TIME then lose_ball("Out of balls") end
    report()
    return
  end
  clock = clock - dt
  if clock <= 0 then
    lose_ball("Out of time")
    report()
    return
  end
  local gx, gy = sable.tilt()
  local steps = 2
  local h = dt / steps
  for _ = 1, steps do
    local kind, what = ground(ball.x, ball.y)
    local ax, ay = gx * PULL, gy * PULL
    if kind == "slope" then ax, ay = ax + what[5], ay + what[6] end
    ball.vx = (ball.vx + ax * h) * (1 - DRAG * h)
    ball.vy = (ball.vy + ay * h) * (1 - DRAG * h)
    local speed = math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
    if speed > TOP then ball.vx, ball.vy = ball.vx * TOP / speed, ball.vy * TOP / speed end
    local ox, oy = 0, 0
    if kind == "mover" then
      -- Carried along with the platform it sits on.
      local x1, y1 = mover_place(what)
      t_all = t_all + h
      local x2, y2 = mover_place(what)
      t_all = t_all - h
      ox, oy = x2 - x1, y2 - y1
    end
    ball.x, ball.y = ball.x + ball.vx * h + ox, ball.y + ball.vy * h + oy
    ball.rx, ball.ry = ball.rx + ball.vx * h / R, ball.ry + ball.vy * h / R
    for _, r in ipairs(lv.rails or {}) do push_out(ball, r[1], r[2], r[3], r[4]) end
  end
  for i, g in ipairs(lv.gems or {}) do
    if not got[i] and (ball.x - g[1]) ^ 2 + (ball.y - g[2]) ^ 2 < (R + 0.3) ^ 2 then
      got[i] = true
      score = score + 10
    end
  end
  if not ground(ball.x, ball.y) then
    falling = 0
  elseif inside(ball.x, ball.y, lv.goal[1], lv.goal[2], lv.goal[3], lv.goal[4], 0) then
    score = score + math.floor(clock) * 2
    cleared = 1.5
  end
  report()
end

-- Drawing ---------------------------------------------------------------------------------------

-- Clouds drift by at two depths: {x, y, size, picture, depth}.
local CLOUDS = {
  { 1, 1.5, 3.2, 0, 0.15 }, { 9, 0.5, 2.6, 1, 0.15 }, { 6, 8, 3.6, 2, 0.15 },
  { 2, 6, 4.4, 1, 0.45 }, { 11, 11, 5, 0, 0.45 },
}

-- The tiles of a platform, cut to whole tiles that stretch a little to fit, and only those in view.
local function tiles(ox, oy, x, y, w, h, kind)
  local nx, ny = math.max(1, math.floor(w + 0.5)), math.max(1, math.floor(h + 0.5))
  local tw, th = w / nx, h / ny
  local i0, i1 = math.max(0, math.floor((ox - 1 - x) / tw)), math.min(nx - 1, math.floor((ox + VIEW + 1 - x) / tw))
  local j0, j1 = math.max(0, math.floor((oy - 1 - y) / th)), math.min(ny - 1, math.floor((oy + VIEW + 1 - y) / th))
  for j = j0, j1 do
    for i = i0, i1 do
      local name = kind
      if not name then name = "stone_" .. ((i * 7 + j * 13 + math.floor(x) * 3 + math.floor(y)) % 3) .. ".png" end
      sable.image(x + i * tw - ox, y + j * th - oy, tw + 0.02, th + 0.02, name)
    end
  end
  return tw, i0, i1
end

-- A floating platform: rock hanging below it, then its tiled top.
local function platform(ox, oy, x, y, w, h, lift)
  if x + w < ox - 1 or x > ox + VIEW + 1 or y + h + 3.4 < oy - 1 or y > oy + VIEW + 1 then return end
  if lift then
    sable.rect(x - ox, y + h - oy, w, 0.22, "#4F6B86")
    sable.rect(x - ox + 0.15, y + h - oy + 0.22, w - 0.3, 0.12, "#7FE3FF99")
    tiles(ox, oy, x, y, w, h, "lift.png")
    return
  end
  sable.image(x - ox, y + h - oy - 0.05, w, math.min(3.4, 0.9 + w * 0.4), "rock_" .. (math.floor(x + y) % 2) .. ".png")
  tiles(ox, oy, x, y, w, h)
end

-- Wind on a slope: a green tint and chevrons that drift the way it pushes.
local function wind(ox, oy, s)
  sable.rect(s[1] - ox, s[2] - oy, s[3], s[4], "#3FBF6A55")
  local px, py = s[5], s[6]
  local n = math.sqrt(px * px + py * py)
  if n == 0 then return end
  local dx, dy = px / n, py / n
  local shift = (t_all * 1.2) % 1
  local span = math.abs(dx) > 0 and s[3] or s[4]
  local across = math.abs(dx) > 0 and s[4] or s[3]
  for k = 0, 3 do
    local f = ((k + shift) / 4) * span - span / 2
    for row = -1, 1 do
      local g = row * across / 3.2
      local cx = s[1] + s[3] / 2 + dx * f + dy * g - ox
      local cy = s[2] + s[4] / 2 + dy * f + dx * g - oy
      local a, b = 0.28, 0.12
      sable.poly({
        cx + dx * a, cy + dy * a,
        cx - dx * a + dy * a, cy - dy * a - dx * a,
        cx - dx * (a - b) , cy - dy * (a - b),
        cx - dx * a - dy * a, cy - dy * a + dx * a,
      }, "#FFFFFFAA")
    end
  end
end

function draw()
  local ox = ball.x + ball.vx * 0.25 - VIEW / 2
  local oy = ball.y + ball.vy * 0.25 - VIEW / 2 + 1.5
  sable.image(0, 0, VIEW, VIEW, "sky.png")
  for _, c in ipairs(CLOUDS) do
    local w = c[3]
    local cx = (c[1] - ox * c[5]) % (VIEW + w + 2) - w - 1
    local cy = (c[2] - oy * c[5]) % (VIEW + w + 2) - w / 2 - 1
    sable.image(cx, cy, w, w / 2, "cloud_" .. c[4] .. ".png")
  end
  -- Far platforms first, so nearer ones (lower on the screen) cover the rock below them.
  local shapes = {}
  for _, f in ipairs(lv.floors) do shapes[#shapes + 1] = { f[1], f[2], f[3], f[4], false } end
  for _, m in ipairs(lv.movers or {}) do
    local mx, my = mover_place(m)
    shapes[#shapes + 1] = { mx, my, m[3], m[4], true }
  end
  table.sort(shapes, function(a, b) return a[2] + a[4] < b[2] + b[4] end)
  for _, s in ipairs(shapes) do platform(ox, oy, s[1], s[2], s[3], s[4], s[5]) end
  for _, s in ipairs(lv.slopes or {}) do wind(ox, oy, s) end
  for _, r in ipairs(lv.rails or {}) do
    sable.rect(r[1] - ox + 0.08, r[2] - oy + 0.22, r[3], r[4], "#00000044")
    sable.image(r[1] - ox, r[2] - oy, r[3], r[4], "rail.png")
  end
  local g = lv.goal
  sable.image(g[1] - ox, g[2] - oy, g[3], g[4], "goal.png")
  local pulse = 1 + math.sin(t_all * 4) * 0.08
  local rs = math.min(g[3], g[4]) * 0.9 * pulse
  sable.image(g[1] + g[3] / 2 - ox - rs / 2, g[2] + g[4] / 2 - oy - rs / 2, rs, rs, "ring.png", (t_all * 60) % 360)
  sable.text(g[1] + g[3] / 2 - ox, g[2] - oy - 0.35, "GOAL", "#FFFFFF", 0.45)
  for i, gem in ipairs(lv.gems or {}) do
    if not got[i] then
      local bob = math.sin(t_all * 3 + i) * 0.08
      local gx, gy = gem[1] - ox, gem[2] - oy
      sable.image(gx - 0.2, gy - 0.05, 0.4, 0.4, "shadow.png")
      sable.image(gx - 0.32, gy - 0.42 + bob, 0.64, 0.64, "gem.png", (t_all * 40 + i * 50) % 360)
      local tw = (t_all * 1.5 + i * 0.37) % 1
      if tw < 0.3 then
        local z = math.sin(tw / 0.3 * math.pi) * 0.45
        sable.image(gx - 0.05 - z / 2, gy - 0.3 + bob - z / 2, z, z, "sparkle.png")
      end
    end
  end
  -- The ball: its shadow (not when falling), the body, a spot that turns as it rolls, and a shine.
  local scale = 1
  if falling then scale = math.max(0.05, 1 - falling / FALL_TIME) end
  local bx, by, r = ball.x - ox, ball.y - oy, R * scale
  if not falling then sable.image(bx - R * 1.1 + 0.12, by - R * 0.9 + 0.22, R * 2.4, R * 2.2, "shadow.png") end
  sable.image(bx - r, by - r, r * 2, r * 2, "ball.png")
  local front = math.cos(ball.rx) * math.cos(ball.ry)
  if front > 0 then
    local sr = 0.15 * scale * (0.45 + 0.55 * front)
    sable.circle(bx + math.sin(ball.rx) * r * 0.62, by + math.sin(ball.ry) * r * 0.62, sr, "#FFFFFFE6")
  end
  if front < 0 then
    local sr = 0.15 * scale * (0.45 - 0.55 * front)
    sable.circle(bx - math.sin(ball.rx) * r * 0.62, by - math.sin(ball.ry) * r * 0.62, sr, "#1E2A44E6")
  end
  sable.image(bx - r, by - r, r * 2, r * 2, "shine.png")
  if cleared then
    sable.rect(0, VIEW / 2 - 3.6, VIEW, 2.2, "#00000055")
    sable.text(VIEW / 2, VIEW / 2 - 2.5, "Goal!", "#FFFFFF", 1.6)
  elseif clock and clock > lv.time - 1.5 and not falling then
    sable.rect(0, VIEW / 2 - 3.7, VIEW, 1.4, "#00000044")
    sable.text(VIEW / 2, VIEW / 2 - 3, lv.name, "#FFFFFF", 0.9)
  end
end
