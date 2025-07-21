# Bill Tracker

## What the Project Does
Bill Tracker is a web-based application for tracking, managing, and analyzing bills for Indian Oil Corporation Limited (IOCL) and other organizations. It is flexible and can be used for any bill or invoice tracking scenario. It allows users to:
- Log in securely (admin and regular users)
- Add, update, and delete bill records
- Upload and download PDF invoices for each bill
- View analytics and reports on bill data
- Filter bills in certain tabs for quick searching
- Export bill data to Excel for further analysis
- Control access based on user roles (admin/user)
- Store all data locally (no external database required)

## How to Use
1. **Build and Run the Application**
   - Make sure you have Java 17+ and Maven installed.
   - In your project folder, run:
     ```sh
     mvn clean package
     mvn spring-boot:run
     ```
   - Open your browser and go to [http://localhost:8081](http://localhost:8081)

2. **Login**
   - Default admin credentials: `admin` / `admin123`
   - You can register new users or reset the admin password using the provided utilities.

3. **Manage Bills**
   - Add new bills, edit existing ones, or delete as needed
   - Upload PDF invoices for each bill (saved in `pdfs/shared/`)
   - Use the filter/search feature in relevant tabs to quickly find bills
   - Export bill data to Excel using the export button in the dashboard
   - View analytics and filter/search bills

4. **Sample Data**
   - To generate sample bills, run:
     ```sh
     javac CreateSampleData.java
     java CreateSampleData
     ```
   - This will create `data/bills.dat` with example entries

5. **Deploy**
   - For Tomcat: Build the WAR and copy to Tomcat's `webapps/` folder
   - For embedded mode: Use `mvn spring-boot:run`

---

## Features
- User authentication (admin and regular users)
- Bill management (add, update, delete, view)
- PDF upload and download for each bill
- Role-based access control
- Analytics and reporting
- **Filter/search bills in certain tabs**
- **Export bill data to Excel**
- Data stored in serialized files (no external database required)
- Sample data generation utility

## Project Structure
- `src/main/java/com/login/` - Main Java source code
  - `controller/` - REST API controllers
  - `model/` - Data models (User, BillRecord, etc.)
  - `service/` - Business logic
  - `util/` - Utilities (sample data, admin reset, etc.)
  - `ui/` - (If present) Java Swing UI classes
- `src/main/resources/static/` - Frontend static files (JS, CSS, images)
- `data/` - Serialized data files (bills, users, etc.)
- `pdfs/` - Uploaded PDF files
- `target/` - Build output (ignored by Git)

## Getting Started

### Prerequisites
- Java 17 or later
- Maven 3.6+

### Build and Run (Spring Boot)
```sh
mvn clean package
mvn spring-boot:run
```
The app will start on [http://localhost:8081](http://localhost:8081) by default.

### Change Port
To use a different port, edit `src/main/resources/application.properties`:
```
server.port=8081
```

### Sample Data
To generate sample bill data, run:
```sh
javac CreateSampleData.java
java CreateSampleData
```
This will create `data/bills.dat` with sample entries.

### PDF Uploads
Uploaded PDFs are saved in the `pdfs/shared/` directory by default.

## Deployment
- For external Tomcat, build the WAR file and deploy to Tomcat's `webapps/` folder.
- For embedded mode, use `mvn spring-boot:run`.

## GitHub Setup
1. Create a repository on GitHub
2. Initialize git, add remote, and push:
   ```sh
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/BAdithya45/BSNL-Tracker.git
   git push -u origin main
   ```

## License
This project is for internal/educational use. Add your license as needed.

---
**Maintainer:** BAdithya45
