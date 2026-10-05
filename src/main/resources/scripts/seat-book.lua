--[[
  Atomic seat booking Lua script for Redis.

  Verifies that ALL seats are HELD by the given reservation_id,
  then transitions them to BOOKED (permanent - no TTL).

  KEYS: seat keys
  ARGV[1]: reservation_id

  Returns:
    "OK" if all seats were booked
    "NOT_HELD:<key>" if a seat is not held by this reservation
    "EXPIRED:<key>" if a seat's hold has expired (key missing)
--]]

local reservationId = ARGV[1]

-- Phase 1: Verify all seats are held by this reservation
for i, key in ipairs(KEYS) do
    local currentValue = redis.call('GET', key)
    if currentValue == nil then
        return 'EXPIRED:' .. key
    end
    local expectedValue = 'HELD:' .. reservationId
    if currentValue ~= expectedValue then
        return 'NOT_HELD:' .. key
    end
end

-- Phase 2: Transition all to BOOKED (remove TTL - permanent state)
for i, key in ipairs(KEYS) do
    redis.call('SET', key, 'BOOKED:' .. reservationId)
    redis.call('PERSIST', key)
end

return 'OK'
