
import spiceypy as spice

spice.furnsh("./kernelData.txt")

step = 1
# we are going to get positions between these two dates
utc = ['Mar 14, 2019', 'Mar 15, 2019']

# get et values one and two, we could vectorize str2et
etOne = spice.str2et(utc[0])
etTwo = spice.str2et(utc[1])
print("ET One: {}, ET Two: {}".format(etOne, etTwo))

# get times
times = [x*(etTwo-etOne)/step + etOne for x in range(step)]

# check first few times:
print(times[0:3])

positions, lightTimes = spice.spkpos('612', times, 'J2000', 'NONE', '10')

# Positions is a 3xN vector of XYZ positions
print("Positions: ")
print(positions[0])

# Light times is a N vector of time
print("Light Times: ")
print(lightTimes[0])