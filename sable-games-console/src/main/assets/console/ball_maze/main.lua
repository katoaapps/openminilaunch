-- Ball Maze for Sable Games Console: the hand-held puzzle with steel balls under a clear lid. Lean
-- the phone and the balls roll; get every ball into the middle at the same time. Choose a round or
-- square board, a plastic or wooden one, and how hard the maze is. A new maze is made each game.
-- The board pictures are drawn for Sable.
local SIZE = 12
local CX, CY = 6, 6
local WALL = 0.13
local WH = WALL / 2
local GRAVITY, DRAG, BOUNCE, TOP_SPEED = 26, 0.8, 0.35, 13

local LEVELS = {
  { name = "Easy", balls = 2, square = 5, room = 1, rings = 3 },
  { name = "Medium", balls = 3, square = 7, room = 3, rings = 4 },
  { name = "Hard", balls = 4, square = 9, room = 3, rings = 5 },
  { name = "Expert", balls = 5, square = 11, room = 3, rings = 6 },
}
local SHAPES = { "Round", "Square" }
local FEELS = { "Plastic", "Wood" }
local ROWS = { "shape", "feel", "level" }

local function saved(key, count, fallback)
  local n = tonumber(sable.load(key) or "") or fallback
  if n < 1 or n > count then n = fallback end
  return math.floor(n)
end

local pick = {
  shape = saved("shape", #SHAPES, 1),
  feel = saved("feel", #FEELS, 1),
  level = saved("level", #LEVELS, 1),
}
local OPTIONS = { shape = SHAPES, feel = FEELS, level = LEVELS }

local mode, row
local segs, arcs, dots, rects, balls, radius, goal, clock, done

-- Walls -------------------------------------------------------------------------------------------

local function add_seg(x1, y1, x2, y2)
  segs[#segs + 1] = {
    x1, y1, x2, y2,
    minx = math.min(x1, x2), maxx = math.max(x1, x2),
    miny = math.min(y1, y2), maxy = math.max(y1, y2),
    len2 = (x2 - x1) ^ 2 + (y2 - y1) ^ 2,
  }
end

local TAU = 2 * math.pi

local function add_arc(r, a0, span)
  arcs[#arcs + 1] = { r = r, a0 = a0 % TAU, span = span }
end

-- A random order of 1..n.
local function shuffled(n)
  local t = {}
  for i = 1, n do t[i] = i end
  for i = n, 2, -1 do
    local j = math.random(i)
    t[i], t[j] = t[j], t[i]
  end
  return t
end

-- Joins every cell into one maze (depth first, so it has long winding paths). `neighbours(c)`
-- lists a cell's neighbours; returns the set of open passages, keyed "a:b" with a < b.
local function carve(cells, start, neighbours)
  local open, seen, stack = {}, { [start] = true }, { start }
  while #stack > 0 do
    local c = stack[#stack]
    local options = {}
    for _, n in ipairs(neighbours(c)) do
      if cells[n] and not seen[n] then options[#options + 1] = n end
    end
    if #options == 0 then
      stack[#stack] = nil
    else
      local n = options[math.random(#options)]
      seen[n] = true
      open[math.min(c, n) .. ":" .. math.max(c, n)] = true
      stack[#stack + 1] = n
    end
  end
  return open
end

local function linked(open, a, b)
  return open[math.min(a, b) .. ":" .. math.max(a, b)] == true
end

-- A square maze: n by n cells over 11 by 11, a room of room by room cells in the middle with one
-- way in.
local function square_maze(level)
  local n, room = level.square, level.room
  local c = 11 / n
  local x0, y0 = CX - 5.5, CY - 5.5
  local lo = math.floor((n - room) / 2)
  local function id(i, j) return j * n + i end
  local function in_room(i, j) return i >= lo and i < lo + room and j >= lo and j < lo + room end
  local cells = {}
  for j = 0, n - 1 do
    for i = 0, n - 1 do
      if not in_room(i, j) then cells[id(i, j)] = true end
    end
  end
  local function neighbours(k)
    local i, j = k % n, math.floor(k / n)
    local out = {}
    for _, d in ipairs(shuffled(4)) do
      local di, dj = ({ 1, -1, 0, 0 })[d], ({ 0, 0, 1, -1 })[d]
      local a, b = i + di, j + dj
      if a >= 0 and a < n and b >= 0 and b < n then out[#out + 1] = id(a, b) end
    end
    return out
  end
  local open = carve(cells, id(0, 0), neighbours)
  -- One door into the room, from a random side.
  local side, along = math.random(4), lo + math.random(room) - 1
  local door
  if side == 1 then door = { id(along, lo - 1), id(along, lo) }
  elseif side == 2 then door = { id(along, lo + room), id(along, lo + room - 1) }
  elseif side == 3 then door = { id(lo - 1, along), id(lo, along) }
  else door = { id(lo + room, along), id(lo + room - 1, along) } end
  open[math.min(door[1], door[2]) .. ":" .. math.max(door[1], door[2])] = true
  local function passable(a, b)
    local i1, j1, i2, j2 = a % n, math.floor(a / n), b % n, math.floor(b / n)
    if in_room(i1, j1) and in_room(i2, j2) then return true end
    return linked(open, a, b)
  end
  -- Walls on the right of and below each cell, then the frame; runs of wall are joined.
  for j = 0, n - 1 do
    local run
    for i = 0, n do
      local wall = i < n and j < n - 1 and not passable(id(i, j), id(i, j + 1))
      if wall and not run then run = i end
      if not wall and run then
        add_seg(x0 + run * c, y0 + (j + 1) * c, x0 + i * c, y0 + (j + 1) * c)
        run = nil
      end
    end
  end
  for i = 0, n - 2 do
    local run
    for j = 0, n do
      local wall = j < n and not passable(id(i, j), id(i + 1, j))
      if wall and not run then run = j end
      if not wall and run then
        add_seg(x0 + (i + 1) * c, y0 + run * c, x0 + (i + 1) * c, y0 + j * c)
        run = nil
      end
    end
  end
  add_seg(x0, y0, x0 + 11, y0)
  add_seg(x0, y0 + 11, x0 + 11, y0 + 11)
  add_seg(x0, y0, x0, y0 + 11)
  add_seg(x0 + 11, y0, x0 + 11, y0 + 11)
  radius = math.max(0.22, math.min(0.45, c * 0.28))
  goal = { x = x0 + lo * c, y = y0 + lo * c, w = room * c }
  -- Balls start in the corners, then half way along the sides.
  local spots = { { 0, 0 }, { n - 1, n - 1 }, { n - 1, 0 }, { 0, n - 1 }, { math.floor(n / 2), 0 } }
  local starts = {}
  for k = 1, level.balls do
    starts[k] = { x0 + (spots[k][1] + 0.5) * c, y0 + (spots[k][2] + 0.5) * c }
  end
  return starts
end

-- A round maze: rings of cells around a round home in the middle, with one way in. Each ring has
-- as many cells as fit, doubling outwards.
local HOME, RIM = 1.1, 5.4

local function round_maze(level)
  local k = level.rings
  local w = (RIM - HOME) / k
  local count, first = {}, {}
  local total = 0
  for i = 1, k do
    if i == 1 then
      count[i] = 6
    else
      local mid = HOME + (i - 0.5) * w
      count[i] = count[i - 1]
      if TAU * mid / count[i] > 2 * w then count[i] = count[i] * 2 end
    end
    first[i] = total
    total = total + count[i]
  end
  local function ring_of(c)
    for i = k, 1, -1 do
      if c >= first[i] then return i, c - first[i] end
    end
  end
  local cells = {}
  for c = 0, total - 1 do cells[c] = true end
  local function neighbours(c)
    local i, j = ring_of(c)
    local out = { first[i] + (j + 1) % count[i], first[i] + (j - 1) % count[i] }
    if i < k then
      if count[i + 1] == count[i] then
        out[#out + 1] = first[i + 1] + j
      else
        out[#out + 1] = first[i + 1] + 2 * j
        out[#out + 1] = first[i + 1] + 2 * j + 1
      end
    end
    if i > 1 then
      if count[i - 1] == count[i] then
        out[#out + 1] = first[i - 1] + j
      else
        out[#out + 1] = first[i - 1] + math.floor(j / 2)
      end
    end
    local order, mixed = shuffled(#out), {}
    for n, o in ipairs(order) do mixed[n] = out[o] end
    return mixed
  end
  local open = carve(cells, first[k], neighbours)
  local door = math.random(count[1]) - 1
  -- The circle between ring i and the ring inside it (the home for ring 1), cell by cell of ring i.
  for i = 1, k do
    local step = TAU / count[i]
    local r = HOME + (i - 1) * w
    local run
    for j = 0, count[i] do
      local wall
      if j < count[i] then
        if i == 1 then
          wall = j ~= door
        else
          local parent = count[i - 1] == count[i] and j or math.floor(j / 2)
          wall = not linked(open, first[i] + j, first[i - 1] + parent)
        end
      end
      if wall and not run then run = j end
      if not wall and run then
        add_arc(r, run * step, (j - run) * step)
        run = nil
      end
    end
    -- Walls across the ring, between neighbouring cells.
    for j = 0, count[i] - 1 do
      local a, b = first[i] + j, first[i] + (j + 1) % count[i]
      if not linked(open, a, b) then
        local ang = (j + 1) * step
        add_seg(CX + r * math.cos(ang), CY + r * math.sin(ang),
          CX + (r + w) * math.cos(ang), CY + (r + w) * math.sin(ang))
      end
    end
  end
  add_arc(RIM + WH, 0, TAU)
  radius = math.max(0.2, math.min(0.4, w * 0.28))
  goal = { r = HOME - WH }
  -- Balls start in the middle of outer-ring cells, spread round the board.
  local starts = {}
  local turn = math.random(count[k]) - 1
  for n = 1, level.balls do
    local j = (turn + math.floor((n - 1) * count[k] / level.balls)) % count[k]
    local ang = (j + 0.5) * TAU / count[k]
    local r = RIM - w / 2
    starts[n] = { CX + r * math.cos(ang), CY + r * math.sin(ang) }
  end
  return starts
end

-- The dots that draw a round board's walls.
local function wall_dots()
  local gap = 0.11
  for _, a in ipairs(arcs) do
    if a.r < RIM then
      local n = math.max(1, math.ceil(a.r * a.span / gap))
      for s = 0, n do
        local ang = a.a0 + a.span * s / n
        dots[#dots + 1] = { CX + a.r * math.cos(ang), CY + a.r * math.sin(ang), a.r < HOME + 0.01 }
      end
    end
  end
  for _, s in ipairs(segs) do
    local n = math.max(1, math.ceil(math.sqrt(s.len2) / gap))
    for t = 0, n do
      dots[#dots + 1] = { s[1] + (s[3] - s[1]) * t / n, s[2] + (s[4] - s[2]) * t / n, false }
    end
  end
end

local function new_maze()
  segs, arcs, dots = {}, {}, {}
  local level = LEVELS[pick.level]
  local starts = pick.shape == 1 and round_maze(level) or square_maze(level)
  if pick.shape == 1 then wall_dots() end
  balls = {}
  for n, p in ipairs(starts) do balls[n] = { x = p[1], y = p[2], vx = 0, vy = 0 } end
  clock, done = 0, false
end

-- Rolling ---------------------------------------------------------------------------------------

local function push(b, px, py, reach)
  local dx, dy = b.x - px, b.y - py
  local d2 = dx * dx + dy * dy
  if d2 >= reach * reach then return end
  local d = math.sqrt(d2)
  if d < 1e-6 then dx, dy, d = 0, -1, 1 end
  local nx, ny = dx / d, dy / d
  local over = reach - d
  b.x, b.y = b.x + nx * over, b.y + ny * over
  local vn = b.vx * nx + b.vy * ny
  if vn < 0 then
    b.vx, b.vy = b.vx - (1 + BOUNCE) * vn * nx, b.vy - (1 + BOUNCE) * vn * ny
  end
end

local function hit_walls(b)
  local reach = radius + WH
  for _, s in ipairs(segs) do
    if b.x > s.minx - reach and b.x < s.maxx + reach and b.y > s.miny - reach and b.y < s.maxy + reach then
      local t = ((b.x - s[1]) * (s[3] - s[1]) + (b.y - s[2]) * (s[4] - s[2])) / s.len2
      t = math.max(0, math.min(1, t))
      push(b, s[1] + (s[3] - s[1]) * t, s[2] + (s[4] - s[2]) * t, reach)
    end
  end
  for _, a in ipairs(arcs) do
    local dx, dy = b.x - CX, b.y - CY
    local d = math.sqrt(dx * dx + dy * dy)
    if math.abs(d - a.r) < reach and d > 1e-6 then
      local rel = (math.atan2(dy, dx) - a.a0) % TAU
      if rel <= a.span then
        push(b, CX + dx / d * a.r, CY + dy / d * a.r, reach)
      else
        push(b, CX + a.r * math.cos(a.a0), CY + a.r * math.sin(a.a0), reach)
        local e = a.a0 + a.span
        push(b, CX + a.r * math.cos(e), CY + a.r * math.sin(e), reach)
      end
    end
  end
end

local function hit_balls()
  local reach = 2 * radius
  for i = 1, #balls - 1 do
    for j = i + 1, #balls do
      local a, b = balls[i], balls[j]
      local dx, dy = b.x - a.x, b.y - a.y
      local d2 = dx * dx + dy * dy
      if d2 < reach * reach and d2 > 1e-9 then
        local d = math.sqrt(d2)
        local nx, ny = dx / d, dy / d
        local over = (reach - d) / 2
        a.x, a.y, b.x, b.y = a.x - nx * over, a.y - ny * over, b.x + nx * over, b.y + ny * over
        local vn = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
        if vn < 0 then
          local j2 = -(1 + BOUNCE) * vn / 2
          a.vx, a.vy = a.vx - j2 * nx, a.vy - j2 * ny
          b.vx, b.vy = b.vx + j2 * nx, b.vy + j2 * ny
        end
      end
    end
  end
end

local function is_home(b)
  if goal.r then
    return (b.x - CX) ^ 2 + (b.y - CY) ^ 2 < (goal.r - radius * 0.6) ^ 2
  end
  local m = radius * 0.4
  return b.x > goal.x + m and b.x < goal.x + goal.w - m and b.y > goal.y + m and b.y < goal.y + goal.w - m
end

local function home_count()
  local n = 0
  for _, b in ipairs(balls) do
    if is_home(b) then n = n + 1 end
  end
  return n
end

-- Screens ---------------------------------------------------------------------------------------

local function report()
  if mode == "menu" then
    sable.status("Up/Down choose · Left/Right change · Space starts")
    return
  end
  sable.status(string.format("%d of %d home · %.1f s · N new maze · M menu", home_count(), #balls, clock))
end

function init()
  sable.grid(SIZE, SIZE)
  mode, row = "menu", 1
  new_maze()
  report()
end

local function start()
  for _, key in ipairs(ROWS) do sable.save(key, tostring(pick[key])) end
  new_maze()
  mode = "play"
  report()
end

function tilt(step)
  if mode ~= "menu" then return end
  if step == "up" then row = (row - 2) % #ROWS + 1
  elseif step == "down" then row = row % #ROWS + 1
  else
    local key = ROWS[row]
    local n = #OPTIONS[key]
    pick[key] = (pick[key] + (step == "right" and 0 or -2)) % n + 1
    new_maze()
  end
  report()
end

function key(name)
  if mode == "menu" then
    if name == "space" or name == "enter" then start() end
  elseif name == "n" and not done then
    new_maze()
    report()
  elseif name == "m" then
    mode = "menu"
    report()
  end
end

function update(dt)
  if mode ~= "play" or done then return end
  dt = math.min(dt, 0.05)
  clock = clock + dt
  local gx, gy = sable.tilt()
  local fastest = 0
  for _, b in ipairs(balls) do
    fastest = math.max(fastest, math.abs(b.vx) + math.abs(b.vy))
  end
  local steps = math.min(8, math.max(1, math.ceil((fastest + GRAVITY * dt) * dt / (radius * 0.6))))
  local h = dt / steps
  for _ = 1, steps do
    for _, b in ipairs(balls) do
      b.vx = (b.vx + gx * GRAVITY * h) * (1 - DRAG * h)
      b.vy = (b.vy + gy * GRAVITY * h) * (1 - DRAG * h)
      local speed = math.sqrt(b.vx * b.vx + b.vy * b.vy)
      if speed > TOP_SPEED then
        b.vx, b.vy = b.vx * TOP_SPEED / speed, b.vy * TOP_SPEED / speed
      end
      b.x, b.y = b.x + b.vx * h, b.y + b.vy * h
      hit_walls(b)
    end
    hit_balls()
    for _, b in ipairs(balls) do hit_walls(b) end
  end
  if home_count() == #balls then
    done = true
    sable.score(math.floor(clock + 0.5))
    sable.over(string.format("All %d home in %.1f s · Space for the menu", #balls, clock))
  end
  report()
end

-- Drawing ---------------------------------------------------------------------------------------

local LOOK = {
  -- Plastic: clear board, blue walls and a red home, like the toy.
  { wall = "#2F5DB8", home = "#D8414C", goal = "#F3D5D8", shadow = "#B9C0C6" },
  -- Wood: dark wooden walls and a brass-coloured home.
  { wall = "#5B3416", home = "#4A2A10", goal = "#E2B26A", shadow = "#8A5A2E" },
}

local function draw_board(x, y, s)
  local name = string.lower(FEELS[pick.feel]) .. "_" .. string.lower(SHAPES[pick.shape]) .. ".png"
  if pick.shape == 1 then
    sable.image(x + 0.4 * s, y + 0.4 * s, 11.2 * s, 11.2 * s, name)
  else
    sable.image(x + 0.3 * s, y + 0.3 * s, 11.4 * s, 11.4 * s, name)
  end
end

-- The maze at scale s, with its top left at x, y (the full board is 12 by 12).
local function draw_maze(x, y, s)
  local look = LOOK[pick.feel]
  draw_board(x, y, s)
  if goal.r then
    sable.circle(x + CX * s, y + CY * s, goal.r * s, look.goal)
    for _, d in ipairs(dots) do
      sable.circle(x + d[1] * s, y + d[2] * s, (WH + 0.015) * s, d[3] and look.home or look.wall)
    end
  else
    sable.rect(x + goal.x * s, y + goal.y * s, goal.w * s, goal.w * s, look.goal)
    for _, w in ipairs(segs) do
      local inner = w.minx > CX - 5.4 and w.maxx < CX + 5.4 and w.miny > CY - 5.4 and w.maxy < CY + 5.4
      if inner or pick.feel == 2 then
        sable.rect(x + (w.minx - WH) * s, y + (w.miny - WH) * s,
          (w.maxx - w.minx + WALL) * s, (w.maxy - w.miny + WALL) * s, look.wall)
      end
    end
  end
end

local function draw_ball(b, x, y, s)
  local r = radius * s
  local bx, by = x + b.x * s, y + b.y * s
  sable.circle(bx + r * 0.25, by + r * 0.3, r, LOOK[pick.feel].shadow)
  sable.circle(bx, by, r, "#7E878E")
  sable.circle(bx - r * 0.18, by - r * 0.2, r * 0.72, "#AEB7BE")
  sable.circle(bx - r * 0.35, by - r * 0.38, r * 0.28, "#F4F7F9")
end

function draw()
  if mode == "menu" then
    sable.rect(0, 0, SIZE, SIZE, "bg")
    sable.text(SIZE / 2, 0.9, "Ball Maze", "text", 1.0)
    draw_maze(3.3, 1.6, 0.45)
    for _, b in ipairs(balls) do draw_ball(b, 3.3, 1.6, 0.45) end
    for n, key in ipairs(ROWS) do
      local y = 7.6 + (n - 1) * 1.35
      if n == row then sable.rect(0.8, y - 0.55, SIZE - 1.6, 1.1, "accent") end
      local label = ({ "Board", "Feel", "Maze" })[n]
      local value = OPTIONS[key][pick[key]]
      value = type(value) == "table" and value.name or value
      local color = n == row and "bg" or "text"
      sable.text(3.2, y, label, color, 0.6)
      sable.text(8.2, y, "‹  " .. value .. "  ›", color, 0.6)
    end
    return
  end
  draw_maze(0, 0, 1)
  for _, b in ipairs(balls) do draw_ball(b, 0, 0, 1) end
end
