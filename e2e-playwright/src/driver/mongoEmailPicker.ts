import { MongoClient } from 'mongodb'

export async function pickRandomEmail(mongoUri: string): Promise<string> {
  const client = new MongoClient(mongoUri)
  await client.connect()

  try {
    const db = client.db()

    const candidateCollections = ['users', 'mflix_users', 'mflixUser', 'mflix']

    for (const colName of candidateCollections) {
      const col = db.collection(colName)
      const doc = await col.find({ email: { $type: 'string' } }).project({ email: 1 }).limit(1).next()
      if (doc?.email) {
        const sampled = await col.aggregate([{ $match: { email: { $type: 'string' } } }, { $sample: { size: 1 } }, { $project: { email: 1 } }]).toArray()
        const email = sampled?.[0]?.email
        if (typeof email === 'string' && email.includes('@')) return email
      }
    }

    throw new Error('Could not find any user emails in expected collections')
  } finally {
    await client.close()
  }
}
