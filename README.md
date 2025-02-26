Task Manager REST API

Overview

This project implements a REST API in Java that provides endpoints for managing and executing "task" objects. Each task represents a shell command that can be executed in a Kubernetes pod. The application uses MongoDB to store task data and provides various endpoints for searching, creating, deleting, and executing tasks.

Features

Create, retrieve, delete, and search tasks.

Execute shell commands securely within a Kubernetes pod.

Store task execution history, including start time, end time, and output.

API endpoints return JSON responses.

Tasks are persisted in a MongoDB database.

Task Object Structure

Each task contains the following properties:

{
  "id": "string",               // Task ID
  "name": "string",             // Task Name
  "owner": "string",            // Task Owner
  "command": "string",          // Shell Command
  "taskExecutions": [
}

API Endpoints

1. Get all tasks or a single task

Endpoint: GET /tasks

Parameters: (Optional) id (Task ID)

Response:

Returns all tasks if no parameters are passed.

Returns a single task if id is provided.

Returns 404 Not Found if no matching task exists.

2. Create a new task

Endpoint: PUT /tasks

Request Body: JSON object containing task details.

Validation:

The command should not contain unsafe/malicious code.

Response:

201 Created if successful.

400 Bad Request if the task ID already exists.

3. Delete a task

Endpoint: DELETE /tasks

Parameters: id (Task ID)

Response:

200 OK if deletion is successful.

404 Not Found if the task does not exist.

4. Search tasks by name

Endpoint: GET /tasks/search

Parameters: name (Search term)

Response:

Returns tasks containing the search term in their name.

404 Not Found if no tasks are found.

5. Execute a task

Endpoint: PUT /tasks/execute

Parameters: id (Task ID)

Response:

Executes the shell command associated with the task.

Stores the execution details in taskExecutions.

Returns 200 OK if execution is successful.

Returns 500 Internal Server Error if execution fails.

Database Configuration

The application uses MongoDB for storing tasks. Ensure MongoDB is running before starting the application.

MongoDB Connection Setup

Modify your application.properties or MongoDB configuration file to include:

mongodb.uri=mongodb://localhost:27017/taskdb

Running the Application

Prerequisites

Java 11+

MongoDB installed and running

Maven installed

Steps to Run

Clone the repository:

git clone https://github.com/your-repo/task-manager-api.git
cd task-manager-api

Build and run the application:

mvn clean install
mvn spring-boot:run

The API will be available at http://localhost:8080/

Testing with Postman

Use Postman to send API requests.

Include the necessary headers (Content-Type: application/json).

Capture request and response screenshots to verify API functionality.

Screenshots

![image](https://github.com/user-attachments/assets/e13e26dc-ee84-48ed-94b0-e0e690c87bf7)
![image](https://github.com/user-attachments/assets/5e9eeb1f-68eb-484a-9528-a45842a6329d)
![image](https://github.com/user-attachments/assets/9484b041-8e2a-4c12-beb0-9e15bcea255f)
![image](https://github.com/user-attachments/assets/5635c8d5-6c5c-47de-ad08-9a0f8b5dac24)



Security Considerations

Validate user inputs to prevent command injection.

Restrict shell command execution to safe commands.

Use authentication/authorization for secure access.

Future Enhancements

Implement authentication and authorization.

Add unit and integration tests.

Support task scheduling.

Author

Preethi

License
Copyright © 2025 Kaiburr LLC. All rights reserved.
