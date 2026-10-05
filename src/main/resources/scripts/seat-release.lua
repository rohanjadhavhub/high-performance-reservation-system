--[[
  Atomic seat release Lua script for Redis.

  Releases seats that are HELD by the given reservation_id.
  Skips seats that are already BOOKED or held by a different reservation.

  KEYS: seat keys
  ARGV[1]: reservation_id

  Returns:
    "OK" with count of released seats
--]]

local reservationId = ARGV[1]
local released = 0

for i, key in ipairs(KEYS) do
    local currentValue = redis.call('GET', key)
    if currentValue ~= nil then
        local expectedValue = 'HELD:' .. reservationId
        if currentValue == expectedValue then
            redis.call('DEL', key)
            released = released + 1
        end
    end
end

return 'OK:' .. released
