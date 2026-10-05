--[[
  Atomic seat hold Lua script for Redis.

  Checks that ALL requested seats are AVAILABLE, then sets them all to HELD
  with a TTL in a single atomic operation. No partial holds allowed.

  KEYS: seat keys (e.g., "show:123:seat:456")
  ARGV[1]: reservation_id
  ARGV[2]: TTL in seconds

  Returns:
    "OK" if all seats were held successfully
    "CONFLICT:<key>" if any seat is already held/booked
--]]

local reservationId = ARGV[1]
local ttlSeconds = tonumber(ARGV[2])

-- Phase 1: Check all seats are available
for i, key in ipairs(KEYS) do
    local currentStatus = redis.call('GET', key)
    if currentStatus ~= nil and currentStatus ~= 'AVAILABLE' then
        return 'CONFLICT:' .. key
    end
end

-- Phase 2: All seats available - atomically set them all to HELD with TTL
for i, key in ipairs(KEYS) do
    redis.call('SET', key, 'HELD:' .. reservationId)
    redis.call('EXPIRE', key, ttlSeconds)
end

return 'OK'
