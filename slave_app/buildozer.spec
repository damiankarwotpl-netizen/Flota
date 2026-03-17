[app]

title = FleetMileageDriver
package.name = fleetmileage
package.domain = org.fleet

source.dir = .
source.include_exts = py,kv,png,jpg

version = 1.0

requirements = python3,kivy,requests,plyer,reportlab

orientation = portrait

fullscreen = 0

android.permissions = INTERNET

android.api = 31
android.minapi = 21
android.sdk = 31

android.ndk = 25b

android.archs = armeabi-v7a, arm64-v8a

log_level = 2


[buildozer]

warn_on_root = 1
