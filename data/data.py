from skyfield.api import load

planets = load('de431t.bsp') #199-499
t = load.timescale().now()


#
indicies = [[199,-1,-1], [299,-1,-1], [399,301,301], [499,401,402], [599,-1,560], [699,-1,653], [799,-1,729], [899,-1,814], [999,-1,905]]

_AU = 1.496e+11


#buffer = []
#file_ = open(strftime("%Y-%m-%d %H-%M-%S", gmtime()) + '.csv', "w")

sun = planets['sun']


print(sun.at(t).observe(planets[399]).position)


for planet in indicies:
    earth = planets[planet[0]]
    #heliocentric = sun.at(t).observe(earth)
    #print(heliocentric.position.km)
    #print(heliocentric.velocity)
    if(planet[1] != -1 and planet[2] != -1):
        for satellite in range(planet[1], planet[2]+1):
            heliocentric = sun.at(t).observe(planets[satellite])
