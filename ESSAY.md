# KotlinConf 2026 Contest Essay

## Federated Urban Insights: A Privacy-First Approach to Urban Mobility Analysis

*By Yunus Emre Çelikkıran*

---

### Introduction

My name is Yunus Emre Çelikkıran, and I am a Computer Engineering student at Istinye University in Istanbul, Turkey. I have been programming for four years, and through this journey, I discovered Kotlin during an internship where it was the primary language. That experience not only taught me Kotlin but also sparked my passion for building practical, user-focused applications.

### The Spark: A Cold Winter Day in Istanbul

The idea for **Federated Urban Insights (FUI)** came from a frustrating personal experience. One cold winter morning, I was waiting for a bus in Istanbul. When it finally arrived, it was so overcrowded that I couldn't get on. As I stood there in the freezing weather, watching the bus drive away, I thought: *"What if I could have known beforehand how crowded it would be?"*

Living in Istanbul, traffic and crowded public transportation are simply part of daily life. With over 15 million residents, the city's mobility challenges affect everyone. This personal frustration became the foundation for FUI—an application that analyzes traffic density and crowd levels to help people make better commuting decisions.

### Why Privacy Matters: A Serverless Philosophy

In today's world, data breaches have become alarmingly common. Almost every week, we hear about another server being compromised and user data being leaked. This reality shaped my approach to FUI fundamentally.

I made a conscious decision to build FUI as a **completely serverless, privacy-first application**. All image analysis happens entirely on the user's device—no data is ever sent to external servers. There is no tracking, no analytics, and no cloud storage of personal images. In an era where privacy is increasingly rare, I believe this approach should become the standard, not the exception.

This philosophy aligns with growing concerns about surveillance and data misuse. By processing everything locally, FUI proves that useful applications can exist without compromising user privacy.

### Technical Journey and Challenges

Building FUI was a significant learning experience. My background was primarily in backend development—I had worked extensively with Kotlin, Vaadin, Spring Boot, and R2DBC. I also had experience with Python (including an OCR project) and Node.js. However, Android development was new territory for me.

The most challenging aspect was **getting the analysis algorithms right**. For a long time, my vehicle detection and crowd estimation produced inaccurate results. I spent countless hours refining the color-blob detection, edge density calculations, and scene classification logic. Each iteration taught me something new about image processing and the importance of testing with diverse real-world scenarios.

Android development was where I learned the most. Coming from a backend background, understanding the Android lifecycle, UI components, and platform-specific considerations was initially overwhelming. But this challenge pushed me to become a more well-rounded developer.

### Kotlin Multiplatform: Write Once, Run Everywhere

One of the most valuable aspects of this project was working with **Kotlin Multiplatform (KMP)**. The shared module in FUI contains the core analysis algorithms—`ImageAnalyzer`, `AnalysisModels`, and `PrivacyContract`—all written once in `commonMain` and used by both the Android and Web applications.

This approach demonstrated KMP's true power: the same `determineTrafficLevel()`, `determineCrowdLevel()`, and `classifyScene()` functions run on Android devices and in web browsers. The consistency this provides is remarkable—users get identical analysis results regardless of their platform.

What I appreciate most about Kotlin is its pragmatism. It's easy to use, expressive, and when I need Java interoperability, it compiles seamlessly to JVM bytecode. This flexibility made building a cross-platform application much more manageable.

### Future Vision

FUI is not a finished product—it's a foundation. My next goal is to complete the **live security camera integration** feature. Imagine being able to check real-time crowd levels at bus stops, train stations, or public spaces before leaving home. This could genuinely improve daily life for millions of commuters.

I plan to continue developing FUI because I believe it can make a real difference. Urban mobility is a challenge that affects billions of people worldwide, and privacy-preserving solutions are more important than ever.

### Personal Growth and Aspirations

This project has been transformative for my development as an engineer. My goal is to become an expert in backend and Android development—someone who can answer any question in these domains with confidence. I want to reach a level where building a project like FUI doesn't consume months of my time, but flows naturally from accumulated expertise.

Kotlin will definitely remain my primary language. Its modern features, excellent tooling, and the expanding KMP ecosystem make it an ideal choice for the multi-platform future of software development.

### Conclusion

FUI represents more than just a contest submission—it's a solution born from real frustration, built with privacy as a core principle, and designed to help people navigate the chaos of urban life. From a cold morning at a bus stop to a fully functional cross-platform application, this journey has taught me that the best software comes from solving problems we personally understand.

Thank you for considering my submission. I hope FUI demonstrates not just technical capability, but also the thoughtful, user-centered approach that I believe should define modern software development.

---

*Yunus Emre Çelikkıran*  
*Istanbul, Turkey*  
*January 2026*

