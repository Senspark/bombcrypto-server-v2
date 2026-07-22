import 'dotenv/config';
import {SignJWT} from 'jose';

const jwtBearerSecret = process.env.JWT_BEARER_SECRET;

if (!jwtBearerSecret) {
  console.error('JWT_BEARER_SECRET must be set in .env');
  process.exit(1);
}

const key = new TextEncoder().encode(jwtBearerSecret);

const token = await new SignJWT({})
  .setProtectedHeader({alg: 'HS256'})
  .setIssuedAt()
  .sign(key);

console.log(token);
