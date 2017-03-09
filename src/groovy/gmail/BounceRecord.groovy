package gmail

import javax.mail.Address;

class BounceRecord {

	Address[] messageTo
	Date date
	
	BounceRecord() {
		super()
	}
	BounceRecord(Address[] to,Date date) {
		this.messageTo=to
		this.date=date
	}
}
