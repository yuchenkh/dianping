local key = KEYS[1]
local value = ARGV[1]
if (redis.call('get', key) == value) then
	return redis.call('del', key)
end
return 0