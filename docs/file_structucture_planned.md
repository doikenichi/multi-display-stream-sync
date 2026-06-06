```
multi-display-stream-sync/
    README.md
    docker-compose.yml
    .gitignore

    scripts/
        run-demo.ps1
        run-demo.sh
        stop-demo.ps1
        stop-demo.sh

    demo/
        sample-report/
        screenshots/
        demo-video.md

    docs/
        architecture.md
        test-strategy.md
        streaming-plan.md
        evidence-format.md

    config/
        test-config.yaml
        mediamtx.yml

    stream-generator/
        Dockerfile
        scripts/
        generate-stream.ps1
        generate-stream.sh
        publish-hls.ps1
        publish-hls.sh

    media-server/
        mediamtx.yml

    display-client/
        Dockerfile
        package.json
        src/
        index.html
        app.js
        styles.css

    reports/
        latest/
            index.html
            summary.json
            offsets.csv
            failure-summary.md
            screenshots/
            display_01_frame.jpg
            display_02_frame.jpg
            display_03_frame.jpg
            logs/
            test-runner.log
            ffmpeg.log
            mediamtx.log

    evidence/
    SYNC_RUN_001/
        summary.json
        offsets.csv
        failure-summary.md
        screenshots/
        logs/

    test-frameworks/
        java/
        pom.xml
        src/
            main/java/
            test/java/

        node/
        package.json
        playwright.config.ts
        src/
            clients/
            capture/
            config/
            evidence/
            marker/
            sync/
            utils/
        tests/
            multi-display-sync.spec.ts

        python/
        pyproject.toml
        pytest.ini
        src/
            streamsync/
            clients/
            capture/
            config/
            evidence/
            marker/
            sync/
            utils/
        tests/
            test_multi_display_sync.py
```

file order creation
1. docs/architecture.md
2. docs/streaming-plan.md
3. docs/test-strategy.md
4. docs/evidence-format.md