package org.giiwa.dfile.command;

import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.dfile.ICommand;
import org.giiwa.dfile.IResponseHandler;
import org.giiwa.dfile.MockRequest;
import org.giiwa.dfile.MockResponse;
import org.giiwa.dfile.Request;
import org.giiwa.dfile.Response;
import org.giiwa.json.JSON;
import org.giiwa.web.Controller;

public class HTTP implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		TimeStamp t = TimeStamp.create();

		String m = in.readString();
		String uri = in.readString();
		JSON head = JSON.fromObject(in.readString());
		JSON body = JSON.fromObject(in.readString());

		// log.debug("head=" + head.toString());
		// log.debug("body=" + body.toString());

		try {
			MockResponse resp = MockResponse.create();
			Controller.process(uri, MockRequest.create(uri, head, body), resp, m, t);

			Response out = Response.create(in.seq, Request.BIG);
			out.writeInt(resp.status);
			out.writeString(resp.head.toString());
			out.writeBytes(resp.out.toByteArray());
			X.close(resp);

			handler.send(out);
		} catch (Exception e) {
			try {
				Response out = Response.create(in.seq, Request.BIG);
				out.writeInt(500);
				out.writeString(X.EMPTY);
				out.writeBytes(e.getMessage().getBytes());
				handler.send(out);
			} catch (Exception e1) {

			}
		} finally {
			if (log.isInfoEnabled())
				log.info(m + " - " + uri + ", cost=" + t.past());
		}
	}

}
