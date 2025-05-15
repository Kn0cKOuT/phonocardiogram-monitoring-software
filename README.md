# Phonocardiogram Monitoring Software

This repository contains the **monitoring software** component of a multidisciplinary phonocardiogram system project. Developed using Java Swing, it visualizes heart sound waveforms and performs basic signal processing such as peak detection and heart rate estimation.

## 🎯 Project Scope

This software is part of a broader project that includes:
- 🩺 Stethoscope design (by Biomedical Engineering team)
- 🔌 Signal conditioning circuit (by Electrical-Electronics Engineering team)
- 🖥️ Monitoring software with GUI and analysis (Computer Engineering — this repo)
- 🧮 Functional programming compliance (Software Engineering)

## 🧩 Features

- Real-time waveform visualization using `JPanel`
- Basic peak detection for heartbeat pulses
- Heart rate (BPM) estimation
- Average amplitude calculation
- Functional programming example using clean separation and reusable logic

## 📂 Folder Structure

```
software/
├── SimpleAudioGraph.java       # Main Java class for GUI & signal logic
├── UML_Diagram.png             # Class diagram (optional)
├── Code_Snippets.txt           # Optional extracted code examples
```

## 🛠 Requirements

- Java JDK 8 or higher
- Java Swing (built-in)

## 🚀 Running the Application

Compile and run the Java file:
```bash
javac SimpleAudioGraph.java
java SimpleAudioGraph
```

The main method simulates an audio signal and displays:
- Estimated heart rate
- Average amplitude
- Waveform panel in GUI

## 👨‍💻 Contributors

| Name              | Department             | Role                     |
|-------------------|-------------------------|--------------------------|
| Your Name         | Computer Engineering    | Monitoring software dev. |
| ...               | Biomedical Engineering  | Stethoscope design       |
| ...               | Electrical Engineering  | Signal conditioning      |
| ...               | Software Engineering    | Functional programming   |

## 📄 License

This project is for academic and educational purposes only. Not for clinical use.

## 📚 References

- Java Swing Documentation: https://docs.oracle.com/javase/tutorial/uiswing/
- IEC 60601-1 Medical Equipment Safety Standard (mentioned in report)
- COM2044 Object-Oriented Programming Course Notes (Ankara University)
