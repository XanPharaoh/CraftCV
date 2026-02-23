import requests, json

r = requests.post(
    "http://127.0.0.1:8000/tailor",
    data={
        "device_id": "test-device-006",
        "job_description": "Senior Python Developer: FastAPI, Docker, AWS.",
        "resume_text": "Software Engineer, 3yr Django+PostgreSQL. REST APIs, Git, Linux."
    }
)
print(f"Status: {r.status_code}")
with open("response.json", "w") as f:
    json.dump(r.json(), f, indent=2)
print("Response saved to response.json")
