import {Request, Response} from "express";

function healthCheckHandler(req: Request, res: Response) {
  res.status(200).send('OK');
}

const simpleHandlers = {
  healthCheckHandler
};

export default simpleHandlers;