# Architecture

Phase 1/2 keeps the diagnostic path isolated in `camera-lab`. `CameraLabViewModel` owns lifecycle-safe background inspection, `CameraReportReader` translates public Camera2 characteristics into a serializable model, and Compose renders the report. The native module is intentionally tiny until image processing is justified by a real device report.
