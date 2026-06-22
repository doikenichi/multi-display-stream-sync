import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import Ajv2020 from "ajv/dist/2020";
import type { AnySchema, ErrorObject } from "ajv";
import addFormats from "ajv-formats";

type JsonValue =
  | string
  | number
  | boolean
  | null
  | JsonValue[]
  | { [key: string]: JsonValue };

const contractsDir = path.resolve(process.cwd(), "..", "..", "contracts");

const evidenceContracts = [
  {
    name: "playback status",
    schemaPath: path.join(
      contractsDir,
      "schemas",
      "playback-status.schema.json",
    ),
    examplePath: path.join(
      contractsDir,
      "examples",
      "playback-status.example.json",
    ),
  },
  {
    name: "sync report",
    schemaPath: path.join(contractsDir, "schemas", "sync-report.schema.json"),
    examplePath: path.join(
      contractsDir,
      "examples",
      "sync-report.example.json",
    ),
  },
];

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

function readSchema(filePath: string): AnySchema {
  return readJson(filePath) as AnySchema;
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

for (const { name, schemaPath, examplePath } of evidenceContracts) {
  const schemaFile = readSchema(schemaPath);
  const example = readJson(examplePath);

  const ajv = new Ajv2020({
    allErrors: true,
    strict: true,
  });

  addFormats(ajv);

  const validate = ajv.compile(schemaFile);
  const isValid = validate(example);

  if (!isValid) {
    console.error(`${name} example does not match the schema:`);
    console.error(formatValidationErrors(validate.errors));
    process.exit(1);
  }

  console.log(`${name} example matches the schema.`);
}
console.log("All evidence contract examples are valid.");
