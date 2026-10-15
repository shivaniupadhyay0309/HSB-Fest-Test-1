from backend.main import app

# This entrypoint enables Vercel and standard ASGI servers to find the app directly at the repository root
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("backend.main:app", host="0.0.0.0", port=8000, reload=True)
