const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-hamed-1d500016-0e68-4bea-922a-398837dd4e2f";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Denies unauthenticated reads and writes", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(unauthedDb.collection("users/user1/messages").get());
  await assertFails(unauthedDb.doc("users/user1/messages/msg1").set({
    userId: "user1",
    sender: "USER",
    text: "سلام",
    sentiment: "POSITIVE",
    createdAt: new Date(),
  }));
});

test("Allows owner to create and read own messages", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore(DATABASE_ID);
  const msgDoc = user1Db.doc("users/user1/messages/msg1");

  await assertSucceeds(msgDoc.set({
    userId: "user1",
    sender: "USER",
    text: "درود بر شما",
    sentiment: "CALM",
    createdAt: new Date(),
  }));

  await assertSucceeds(msgDoc.get());
});

test("Denies cross-user access", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore(DATABASE_ID);
  const user2Db = testEnv.authenticatedContext("user2").firestore(DATABASE_ID);

  await user1Db.doc("users/user1/messages/msg1").set({
    userId: "user1",
    sender: "USER",
    text: "پیام خصوصی",
    sentiment: "POSITIVE",
    createdAt: new Date(),
  });

  // User 2 cannot read or write user 1's messages
  await assertFails(user2Db.doc("users/user1/messages/msg1").get());
  await assertFails(user2Db.doc("users/user1/messages/msg2").set({
    userId: "user1",
    sender: "USER",
    text: "حمله",
    sentiment: "FRUSTRATED",
    createdAt: new Date(),
  }));
});

test("Rejects messages with invalid schema", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore(DATABASE_ID);
  // Missing text
  await assertFails(user1Db.doc("users/user1/messages/bad1").set({
    userId: "user1",
    sender: "USER",
    sentiment: "POSITIVE",
    createdAt: new Date(),
  }));

  // Invalid sender enum
  await assertFails(user1Db.doc("users/user1/messages/bad2").set({
    userId: "user1",
    sender: "INVALID_SENDER",
    text: "تست",
    sentiment: "POSITIVE",
    createdAt: new Date(),
  }));
});
