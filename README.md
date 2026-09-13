# VideoProcessor

A Spring Boot application that processes uploaded video files using native FFmpeg, attaches the generated thumbnail directly into the video container, and persists the files to Azure Blob Storage and SQL Server.

## Architecture & Code Flow

1. **Upload & Ingestion (`VideoController.java`)**
   - Receives video payloads through a `MultipartFile` endpoint.
   - Buffers files temporarily to the OS temp directory using `File.createTempFile` to avoid high memory spikes.

2. **Processing Pipeline (`ProcessBuilder`)**
   - Spawns isolated native FFmpeg sub-processes.
   - Extracts frame 100 as a JPEG thumbnail:
     ```bash
     ffmpeg -i input.mp4 -vf "thumbnail=100,scale=480:-1" -frames:v 1 thumb.jpg
     ```
   - Injects the generated image back into the video as an attached cover (`attached_pic`) using stream copy (`-c copy`), keeping original quality without re-encoding overhead.

3. **Storage & Database Persistence**
   - Uploads both the cover-injected video and standalone thumbnail to Microsoft Azure Blob Storage using `BlobServiceClient` and `BlobClient`.
   - Saves file metadata, Azure Blob URLs, and original filenames in SQL Server via Spring Data JPA (`VideoRepository`).

## Tech Stack

- **Backend**: Java 17, Spring Boot (Spring MVC, Spring Data JPA)
- **Video Engine**: FFmpeg (system CLI called via `ProcessBuilder`)
- **Cloud SDK**: Azure Storage Blob SDK (`azure-storage-blob`)
- **Database**: Microsoft SQL Server
- **Build**: Maven

## Getting Started

### Prerequisites
- JDK 17 or higher
- Maven
- FFmpeg installed and configured in your system `PATH`
- Running SQL Server instance
- Azure Blob Storage account

### Configuration

Edit `src/main/resources/application.properties`:

```properties
server.port=8081

# Azure Storage
azure.storage.connection-string=YOUR_AZURE_CONNECTION_STRING
azure.storage.container-name=videos

# Database
spring.datasource.url=jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=VideoProcessorDB;encrypt=true;trustServerCertificate=true;
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD
