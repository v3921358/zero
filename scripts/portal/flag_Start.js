function enter(pi) {
	switch(pi.getPlayer().getMapId())
	{

		case 932200200:
		pi.teleport(1);
		break;

		default:
	        pi.teleport(16);
		break;
	}
}
