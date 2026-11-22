-- Motion 2048 for Sable Games Console: one tilt (or arrow key) slides the board once; the phone
-- has to come back near level before the next tilt counts.
local N = 4
local board, score, best_tile, won

local COLORS = {
  [2] = "#3A4A57", [4] = "#3F5A6E", [8] = "#E07A2E", [16] = "#E8662E", [32] = "#E5493A",
  [64] = "#D93636", [128] = "#D9B23A", [256] = "#D9A92E", [512] = "#D99F22", [1024] = "#2E9BD6",
  [2048] = "#2F8F5B",
}

local function empty_cells()
  local out = {}
  for y = 1, N do for x = 1, N do if board[y][x] == 0 then out[#out + 1] = { x, y } end end end
  return out
end

local function spawn()
  local free = empty_cells()
  if #free == 0 then return end
  local c = free[math.random(#free)]
  board[c[2]][c[1]] = (math.random() < 0.9) and 2 or 4
end

-- Slides one line toward its start; returns the new line and the points it made.
local function slide(line)
  local tiles, out, points = {}, {}, 0
  for _, v in ipairs(line) do if v > 0 then tiles[#tiles + 1] = v end end
  local i = 1
  while i <= #tiles do
    if tiles[i + 1] and tiles[i] == tiles[i + 1] then
      out[#out + 1] = tiles[i] * 2
      points = points + tiles[i] * 2
      i = i + 2
    else
      out[#out + 1] = tiles[i]
      i = i + 1
    end
  end
  while #out < N do out[#out + 1] = 0 end
  return out, points
end

local function line_of(step, k)
  local cells = {}
  for i = 1, N do
    if step == "left" then cells[i] = { i, k }
    elseif step == "right" then cells[i] = { N + 1 - i, k }
    elseif step == "up" then cells[i] = { k, i }
    else cells[i] = { k, N + 1 - i } end
  end
  return cells
end

local function can_move()
  if #empty_cells() > 0 then return true end
  for y = 1, N do
    for x = 1, N do
      local v = board[y][x]
      if (x < N and board[y][x + 1] == v) or (y < N and board[y + 1][x] == v) then return true end
    end
  end
  return false
end

local function report()
  sable.status(string.format("Score %d · best tile %d", score, best_tile))
  sable.score(score)
end

function init()
  board = {}
  for y = 1, N do board[y] = { 0, 0, 0, 0 } end
  score, best_tile, won = 0, 2, false
  spawn(); spawn()
  sable.grid(N, N)
  report()
end

function tilt(step)
  local moved = false
  for k = 1, N do
    local cells = line_of(step, k)
    local line = {}
    for i, c in ipairs(cells) do line[i] = board[c[2]][c[1]] end
    local out, points = slide(line)
    score = score + points
    for i, c in ipairs(cells) do
      if board[c[2]][c[1]] ~= out[i] then moved = true end
      board[c[2]][c[1]] = out[i]
      if out[i] > best_tile then best_tile = out[i] end
    end
  end
  if not moved then return end
  spawn()
  report()
  if best_tile >= 2048 and not won then
    won = true
    sable.status(string.format("2048! Score %d · keep going", score))
  end
  if not can_move() then sable.over(string.format("No moves left · score %d", score)) end
end

function draw()
  sable.rect(0, 0, N, N, "bg")
  for y = 1, N do
    for x = 1, N do
      local v = board[y][x]
      sable.rect(x - 0.95, y - 0.95, 0.9, 0.9, v > 0 and (COLORS[v] or "#2F8F5B") or "floor")
      if v > 0 then
        local digits = #tostring(v)
        local size = digits <= 2 and 0.42 or (digits == 3 and 0.34 or 0.27)
        sable.text(x - 0.5, y - 0.5, tostring(v), "#FFFFFF", size)
      end
    end
  end
end
