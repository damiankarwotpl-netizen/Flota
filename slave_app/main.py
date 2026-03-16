from kivy.app import App
from kivy.lang import Builder
from slave_sync import SlaveSync

KV = '''

BoxLayout:

    orientation: "vertical"
    padding: 20
    spacing: 10

    TextInput:
        id: name
        hint_text: "Imię"

    TextInput:
        id: surname
        hint_text: "Nazwisko"

    Button:
        text: "Zaloguj"
        on_press: app.login()

    Label:
        id: car
        text: "Samochód: -"

    TextInput:
        id: mileage
        hint_text: "Przebieg"

    Button:
        text: "Wyślij przebieg"
        on_press: app.send()

'''

class DriverApp(App):

    def build(self):

        self.sync = SlaveSync()

        return Builder.load_string(KV)

    def login(self):

        name = self.root.ids.name.text
        surname = self.root.ids.surname.text

        car = self.sync.get_car(name, surname)

        self.car = car

        self.root.ids.car.text = "Samochód: " + str(car)

    def send(self):

        mileage = self.root.ids.mileage.text

        self.sync.send(self.car, mileage)

DriverApp().run()
