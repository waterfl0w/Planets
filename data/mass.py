from astropy.time import Time
from astropy.coordinates import solar_system_ephemeris, EarthLocation
from astropy.coordinates import get_body_barycentric, get_body, get_moon
t = Time("2019-03-14 00:00:00")
with solar_system_ephemeris.set('de430'):
    jup = get_body_barycentric('Earth', t, 'Sun') 

print(jup) 
