# AutoKeeper

An Android auto bookkeeping assistant with local network sync.

## Features

- 📊 Personal finance tracking
- 🔄 Local network synchronization
- 📈 Visual expense analytics
- 📱 Material Design UI

## Tech Stack

- **Android**: Kotlin, Jetpack Compose, Room Database
- **Backend**: Python Flask
- **Design**: Material Design 3

## Project Structure

```
autokeeper/
├── app/              # Android application
├── backend/          # Flask backend
├── server/           # Local sync server
└── docs/             # Documentation
```

## Getting Started

### Android App

```bash
./gradlew assembleDebug
```

### Backend Server

```bash
cd backend
pip install -r requirements.txt
python app.py
```

## License

MIT