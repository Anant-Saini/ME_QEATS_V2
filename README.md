# 🍔 QEats V2 - Restaurant Discovery Platform

<div align="center">
  <strong>A modern restaurant discovery application</strong>
  <br><br>
  <a href="#-features">Features</a> •
  <a href="#-tech-stack">Tech Stack</a> •
  <a href="#-getting-started">Getting Started</a> •
  <a href="#-project-structure">Project Structure</a> •
  <a href="#-contributing">Contributing</a>
</div>

---

## 📋 Overview

QEats V2 is an advanced version of the restaurant discovery platform, built with modern backend technologies and best practices. This project demonstrates a scalable, maintainable architecture with focus on performance, reliability, and clean code principles.

## ✨ Features

- 🔍 **Advanced Search** - Find restaurants by restaurant name, restaurant type, cuisine served, cuisine type etc
- 🏪 **Restaurant Browsing** - Explore restaurants which are nearby and open currently 
- 🗺️ **Geolocation** - Location-based restaurant discovery
- 📊 **Multi-threaded Processing** - Optimized concurrent request handling

## 🛠️ Tech Stack

### Backend
- **Language**: Java 11+
- **Framework**: Spring Boot
- **Build Tool**: Gradle
- **Database**: MongoDB
- **Testing**: JUnit, Mockito
- **API Testing**: Postman

### Key Libraries
- Spring Data MongoDB for database operations
- Lombok for reducing boilerplate code
- ModelMapper for object transformations
- Log4j for logging

## 🚀 Getting Started

### Prerequisites
- Java 11 or higher
- Gradle 6.0+
- MongoDB installed and running
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone https://gitlab.crio.do/COHORT_ME_QEATS_V2_ENROLL_1642584294077/anantsaini-india-ME_QEATS_V2.git
   cd anantsaini-india-ME_QEATS_V2
   ```

2. **Build the project**
   ```bash
   gradle clean build
   ```

3. **Run the application**
   ```bash
   gradle bootRun
   ```

4. **Access the API**
   - Base URL: `http://localhost:8081`
   - API Documentation: Check Postman collections in the repository

## 📁 Project Structure

```
├── qeatsbackend/              # Main backend module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── controller/      # REST API endpoints
│   │   │   │   ├── service/         # Business logic
│   │   │   │   ├── repository/      # Data access layer
│   │   │   │   ├── model/           # Entity classes
│   │   │   │   ├── dto/             # Data transfer objects
│   │   │   │   └── util/            # Utility classes
│   │   │   └── resources/
│   │   │       └── application.yml   # Configuration
│   │   └── test/                    # Unit and integration tests
│   └── build.gradle                 # Dependencies
├── gradle/
├── .gitignore
└── README.md
```

## 🔧 Configuration

### MongoDB Connection
Update `application.yml` with your MongoDB connection details:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/qeats
```

## 📝 API Endpoints

### Restaurants
- `GET /restaurants?latitude={latitude}&longitude={longitude}` - Get nearby restaurants
- `GET /restaurants?latitude={latitude}&longitude={longitude}&searchFor={searchFor}` - Filter nearby restaurants based on search string 
- `GET /restaurants/{id}/menu` - Get restaurant menu

## 🧪 Testing

Run all tests:
```bash
gradle test
```

Run specific test class:
```bash
gradle test --tests TestClassName
```

Generate coverage report:
```bash
gradle test jacocoTestReport
```

## 🏗️ Architecture Highlights

- **Clean Architecture** - Separation of concerns with clear layer boundaries
- **Repository Pattern** - Abstraction for data access
- **Service Layer** - Business logic encapsulation
- **DTO Pattern** - Data transfer between layers
- **Multi-threading** - Concurrent request processing for improved performance
- **Proper Error Handling** - Comprehensive exception handling and validation

## 💡 Key Learning Points

- Spring Boot application development
- MongoDB operations and queries
- RESTful API design principles
- Unit testing with JUnit and Mockito
- Gradle build automation
- Object-Oriented Design patterns
- Concurrent programming in Java

## 📚 Documentation

For more detailed documentation, please refer to:
- API Documentation (Postman collection included)
- Code comments in the codebase
- MongoDB query examples in the repository

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Create a feature branch (`git checkout -b feature/amazing-feature`)
2. Commit your changes (`git commit -m 'Add amazing feature'`)
3. Push to the branch (`git push origin feature/amazing-feature`)
4. Open a Pull Request

## ✅ Best Practices

- Write clean, readable code
- Follow Java naming conventions
- Add meaningful commit messages
- Write unit tests for new features
- Keep methods small and focused
- Use meaningful variable names
- Document complex logic

## 📞 Support

For issues or questions:
- Open an issue in the repository
- Check existing documentation
- Review code comments for clarification

## 📄 License

This project is part of the Crio.Do curriculum. All rights reserved.

---

<div align="center">
  <strong>Made with ❤️ as part of Crio.Do Backend Engineering Bootcamp</strong>
  <br><br>
  <sub>Happy Coding! 🚀</sub>
</div>
