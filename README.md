# ShowPlayTime and Timer

ShowPlayTime is a highly customizable client-side mod for Minecraft (Fabric) that gives you full control over your playtime and timers. Whether for challenges, speedruns, or just keeping track of your session, this mod provides all the tools you need.

## Main Features

* World-based Storage: Each world and server has its own independent playtime and timer state.
* Statistics Import: When first entering a world, the mod automatically imports your existing playtime from the official Minecraft statistics.
* Animated Gradients: Create smooth, flowing color transitions for your HUD display.
* Smart Sounds: Audio feedback for actions like starting, pausing, or finishing (can be muted).
* Auto-Pause: The timer automatically pauses when you leave a server or close the world.
* Countdown Alarm: A celebration sound plays when a countdown reaches zero.

## Commands and Functions

### Timer Control (/timer)
| Command | Function |
| :--- | :--- |
| /timer start | Starts the timer (blocked if already running). |
| /timer pause | Pauses the timer (HUD shows "paused"). |
| /timer resume | Resumes a paused timer. |
| /timer stop | Stops the timer and resets it to zero. |
| /timer set <h> <m> <s> | Sets the timer to an exact time. |
| /timer add <h> <m> <s> | Adds time to the current timer. |
| /timer backwards <true/false> | Enables or disables countdown mode. |
| /timer show <true/false> | Shows or hides the timer in the HUD. |

### Playtime (/playtime)
| Command | Function |
| :--- | :--- |
| /playtime | Displays your current world playtime in chat. |
| /playtime show <true/false> | Enables the permanent playtime display in the HUD. |

### HUD Design (/timerlook)
* Text Effects: /timerlook bold, italic, or underlined (true/false).
* Position: /timerlook ypos <value> (moves the HUD vertically).
* Audio: /timerlook mutesound <true/false> (disables all mod sounds).
* Gradient Colors:
    * /timerlook color add <color> - Adds a color to the gradient.
    * /timerlook color remove <color> - Removes a specific color.
    * /timerlook color clear - Resets colors to plain white.
* Reset: /timerlook reset - Resets all visual settings to default.

---

# ShowPlayTime und Timer (Deutsch)

ShowPlayTime ist eine hochgradig anpassbare Client-Side Mod für Minecraft (Fabric), die dir die volle Kontrolle über deine Spielzeit und Timer gibt. Egal ob für Challenges, Speedruns oder einfach nur zur Übersicht – diese Mod bietet alle nötigen Funktionen.

## Haupt-Features

* Weltbasierte Speicherung: Jede Welt und jeder Server hat eine eigene Spielzeit und einen eigenen Timer-Status.
* Statistik-Import: Beim ersten Betreten einer Welt importiert die Mod automatisch deine bisherige Spielzeit aus den offiziellen Minecraft-Statistiken.
* Animierte Verläufe: Erstelle fließende Farbverläufe für dein HUD.
* Smart Sounds: Akustisches Feedback bei Aktionen (abschaltbar).
* Auto-Pause: Der Timer pausiert automatisch, wenn du den Server verlässt oder die Welt schließt.
* Countdown-Alarm: Ein Sound ertönt, sobald ein Countdown bei Null ankommt.

## Befehle und Funktionen

### Timer-Steuerung (/timer)
| Befehl | Funktion |
| :--- | :--- |
| /timer start | Startet den Timer (blockiert, wenn er bereits läuft). |
| /timer pause | Pausiert den Timer (HUD zeigt "paused"). |
| /timer resume | Setzt einen pausierten Timer fort. |
| /timer stop | Stoppt den Timer und setzt ihn auf 0 zurück. |
| /timer set <h> <m> <s> | Setzt den Timer auf eine exakte Zeit. |
| /timer add <h> <m> <s> | Addiert Zeit zum aktuellen Timer. |
| /timer backwards <true/false> | Aktiviert oder deaktiviert den Countdown-Modus. |
| /timer show <true/false> | Zeigt oder versteckt den Timer im HUD. |

### Spielzeit (/playtime)
| Befehl | Funktion |
| :--- | :--- |
| /playtime | Zeigt deine aktuelle Welt-Spielzeit im Chat an. |
| /playtime show <true/false> | Aktiviert die permanente Anzeige der Spielzeit im HUD. |

### HUD-Design (/timerlook)
* Texteffekte: /timerlook bold, italic oder underlined (true/false).
* Position: /timerlook ypos <wert> (verschiebt das HUD vertikal).
* Audio: /timerlook mutesound <true/false> (deaktiviert alle Mod-Sounds).
* Farbverlauf (Gradient):
    * /timerlook color add <farbe> - Fügt eine Farbe zum Verlauf hinzu.
    * /timerlook color remove <farbe> - Entfernt eine Farbe.
    * /timerlook color clear - Setzt die Farben auf Weiß zurück.
* Reset: /timerlook reset - Setzt alle Optik-Einstellungen zurück.
