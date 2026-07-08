import fs from "node:fs";
import path from "node:path";
import process from "node:process";

import Ajv2020 from "ajv/dist/2020";
import type { AnySchema, ErrorObject } from "ajv";
import addFormats from "ajv-formats";
import YAML from "yaml";

type JsonValue =
  | string
  | number
  | boolean
  | null
  | JsonValue[]
  | { [key: string]: JsonValue };

type ContractExample = {
  name: string;
  schemaPath: string;
  examplePath: string;
  format: "json" | "yaml";
};

const contractDir = path.resolve(process.cwd(), "..", "..", "contract");

const contractExamples: ContractExample[] = [
  {
    name: "playback status",
    schemaPath: path.join(contractDir, "schemas", "playback-status.schema.json"),
    examplePath: path.join(contractDir, "examples", "playback-status.example.json"),
    format: "json",
  },
  {
    name: "run summary",
    schemaPath: path.join(contractDir, "schemas", "run-summary.schema.json"),
    examplePath: path.join(contractDir, "examples", "run-summary.example.json"),
    format: "json",
  },
  {
    name: "local test framework config",
    schemaPath: path.join(contractDir, "schemas", "test-framework-config.schema.json"),
    examplePath: path.join(contractDir, "examples", "test-framework-local.yaml"),
    format: "yaml",
  },
  {
    name: "ci test framework config",
    schemaPath: path.join(contractDir, "schemas", "test-framework-config.schema.json"),
    examplePath: path.join(contractDir, "examples", "test-framework-ci.yaml"),
    format: "yaml",
  },
];

function readText(filePath: string): string {
  try {
    return fs.readFileSync(filePath, "utf8");
  } catch (error: unknown) {
    console.error(`Failed to read file: ${filePath}`);
    if (error instanceof Error) {
      console.error(error.message);
    } else {
      console.error(error);
    }
    process.exit(1);
  }
}

function readJson(filePath: string): JsonValue {
  try {
    return JSON.parse(readText(filePath)) as JsonValue;
  } catch (error: unknown) {
    console.error(`Failed to parse JSON: ${filePath}`);
    if (error instanceof Error) {
      console.error(error.message);
    } else {
      console.error(error);
    }
    process.exit(1);
  }
}

function readYaml(filePath: string): JsonValue {
  try {
    return YAML.parse(readText(filePath)) as JsonValue;
  } catch (error: unknown) {
    console.error(`Failed to parse YAML: ${filePath}`);
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

function readExample(examplePath: string, format: "json" | "yaml"): JsonValue {
  return format === "json" ? readJson(examplePath) : readYaml(examplePath);
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

for (const { name, schemaPath, examplePath, format } of contractExamples) {
  const schemaFile = readSchema(schemaPath);
  const example = readExample(examplePath, format);
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

console.log("All contract examples are valid.");
