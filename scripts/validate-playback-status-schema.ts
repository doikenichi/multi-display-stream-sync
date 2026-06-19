import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import Ajv2020 from "ajv/dist/2020";
import type { ErrorObject } from "ajv";
import addFormats from "ajv-formats";

type JsonValue =
  | string
  | number
  | boolean
  | null
  | JsonValue[]
  | { [key: string]: JsonValue };

const rootDir = process.cwd();

const schemaPath = path.join(
  rootDir,
  "docs",
  "templates",
  "playback-status.schema.json",
);

const examplePath = path.join(
  rootDir,
  "docs",
  "templates",
  "playback-status.example.json",
);

function readJson(filePath: string): JsonValue {
  try {
    return JSON.parse(fs.readFileSync(filePath, "utf8")) as JsonValue;
  } catch (error: unknown) {
    console.error(`Failed to read or parse JSON: ${filePath}`);

    if (error instanceof Error) {
      console.error(error.message);
    } else {
      console.error(error);
    }

    process.exit(1);
  }
}

function formatValidationErrors(
  errors: ErrorObject[] | null | undefined,
): string {
  if (!errors || errors.length === 0) {
    return "No validation error details available.";
  }

  return errors
    .map((error) => {
      const location = error.instancePath || "/";
      return `- ${location}: ${error.message}`;
    })
    .join("\n");
}

const schema = readJson(schemaPath);
const example = readJson(examplePath);

const ajv = new Ajv2020({
  allErrors: true,
  strict: true,
});

addFormats(ajv);

const validate = ajv.compile(schema);
const isValid = validate(example);

if (!isValid) {
  console.error("Playback status example does not match the schema:");
  console.error(formatValidationErrors(validate.errors));
  process.exit(1);
}

console.log("Playback status example matches the schema.");
