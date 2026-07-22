import IDependencies from './IDependencies';
import jwt from 'jsonwebtoken';

export default class JwtService {
  constructor(
    private readonly _dep: IDependencies
  ) {
    this.#secret = _dep.envConfig.jwtSecret;
    this.#payloadKey = _dep.envConfig.jwtPayloadKey;
  }

  readonly #secret: string;
  readonly #payloadKey: string;

  sign() {
    const payload: Payload = {key: this.#payloadKey};
    return jwt.sign(payload, this.#secret);
  }

  verify(token: string): boolean {
    try {
      if (token.startsWith('Bearer ')) {
        token = token.replace('Bearer ', '');
      }
      const payload = jwt.verify(token, this.#secret) as Payload;
      return payload.key === this.#payloadKey;
    } catch (e) {
      this._dep.logger.error(e);
      return false;
    }
  }
}

type Payload = {
  key: string
}
