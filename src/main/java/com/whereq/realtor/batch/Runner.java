package com.whereq.realtor.batch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.whereq.realtor.batch.domain.FH_ListingAddrPO;
import com.whereq.realtor.batch.domain.FH_ListingExtraPO;
import com.whereq.realtor.batch.domain.FH_PropertyDetailsPO;
import com.whereq.realtor.batch.domain.FH_PropertyPO;
import com.whereq.realtor.batch.domain.FH_PropertyRoomsPO;
import com.whereq.realtor.batch.domain.ListingFullPO;
import com.whereq.realtor.batch.domain.ListingActivePO;
import com.whereq.realtor.batch.repository.AddressRepository;
import com.whereq.realtor.batch.repository.FHPropertyDetailsRepository;
import com.whereq.realtor.batch.repository.FHPropertyRepository;
import com.whereq.realtor.batch.repository.FHPropertyRoomsRepository;
import com.whereq.realtor.batch.repository.ListingExtraRepository;
import com.whereq.realtor.batch.repository.ListingRepository;
import com.whereq.realtor.xml.bind.FullResListing;
import com.whereq.realtor.xml.bind.FullResidentialProperty;
import com.whereq.realtor.xml.bind.FullResidentialPropertyWrapper;

@Component(value="runner")
public class Runner {

	@Inject
	private ListingRepository repository;

	@Inject
	private AddressRepository add_repository;
	
	@Inject
	private ListingExtraRepository extra_repository;
	
	@Inject
	private FHPropertyRepository property_repository;
	
	@Inject
	private FHPropertyDetailsRepository pptDetails_repository;

	@Inject
	private FHPropertyRoomsRepository pptRooms_repository;
	
    @Resource
    private Environment env;
	
	
	public void run() throws JAXBException, XMLStreamException{
		
		Logger logger = LoggerFactory.getLogger(Runner.class);	

		
		List<ListingFullPO> poList = Lists.newArrayList();
		
		List<FH_ListingAddrPO> addList = Lists.newArrayList();
		
		List<FH_ListingExtraPO> extraList = Lists.newArrayList();

		List<FH_PropertyPO> ppyList = Lists.newArrayList();
		
		List<FH_PropertyDetailsPO> ppyDetailsList = Lists.newArrayList();

		List<FH_PropertyRoomsPO> ppyRoomList = Lists.newArrayList();
		
		System.out.println("============================================================");
		System.out.println("Start: " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime()));
		
        JAXBContext jc = JAXBContext.newInstance(FullResidentialPropertyWrapper.class);
        System.out.println("jaxbContent class: " + jc.getClass());

        XMLInputFactory xif = XMLInputFactory.newFactory();
        String xmlpath = env.getProperty("FULL_RES_PATH");
        System.out.println("env: " + xmlpath);
        XMLStreamReader xsr = xif.createXMLStreamReader(new StreamSource(xmlpath));
        //XMLStreamReader xsr = xif.createXMLStreamReader(new StreamSource("C:/tmp/full_residential.xml"));


        Unmarshaller unmarshaller = jc.createUnmarshaller();
        FullResidentialPropertyWrapper  rets = (FullResidentialPropertyWrapper) unmarshaller.unmarshal(xsr );
        
        int i = 0;
        int j = 0;
        int pc = 0;
		for (FullResidentialProperty residentialProperty : rets.getActResProperties()) {
			{
				FullResListing listFromXml = residentialProperty.getListing();
				String mls = listFromXml.getMls(); 
				ListingFullPO exist = repository.findByMLS(mls);
				if (exist == null)
				{
					i++;
					poList.add(saveIntoListingTable(listFromXml)) ;
					addList.add(saveIntoAddressTable(listFromXml));
					extraList.add(saveIntoExtraTable(listFromXml));
					ppyList.add(saveIntoPropertyTable(listFromXml));
					ppyDetailsList.add(saveIntoPropertyDetailsTable(listFromXml));
					ppyRoomList.add(saveIntoPropertyRoomsTable(listFromXml));
					
				}
				else
				{
					System.out.println("MLS#: " + mls + " existing alreay, check to see if pc changed ...." );
					System.out.println("old price: " + exist.getListPrice() + ", new price: " + listFromXml.getListPrice()); 
					if (exist.getListPrice() != listFromXml.getListPrice()) 
					{
						exist.setListPrice(listFromXml.getListPrice());
						exist.setStatus("Pc");
						repository.save(exist);
						System.out.println("update mls: " + mls + " to the new price.");
						pc++;
					}
					j++;
				}

			}	
		}
		
		repository.save(poList);
		
		add_repository.save(addList);
		
		extra_repository.save(extraList);
		
		property_repository.save(ppyList);
		
		pptDetails_repository.save(ppyDetailsList);
		
		pptRooms_repository.save(ppyRoomList);
		
		
		System.out.println("Total: ["+ i + "] inserted on  fh_listing table, [" + j + "] listings exist in the table already, [" + pc +"] listings changed prices!");
		System.out.println("End: " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime()));
		System.out.println("============================================================");

		
	}
	
	
	private ListingFullPO saveIntoListingTable(FullResListing listingFull)
	{
		ListingFullPO listingPO = null;
		
		//fh_listing
		System.out.println(listingFull.getMls() + " into fr_listing table:");
		listingPO = new ListingFullPO();
		listingPO.setMLS(listingFull.getMls());
		listingPO.setAddress(listingFull.getAddress());
		listingPO.setListPrice(listingFull.getListPrice());
		listingPO.setSaleLease(listingFull.getSaleLease());
		
		listingPO.setLegalDescription(listingFull.getLegalDescription());
		listingPO.setTaxes(listingFull.getTaxes());
		listingPO.setTaxYear(listingFull.getTaxYear());
		listingPO.setPostalCode(listingFull.getPostalCode());
		listingPO.setMunicipalityDistrict(listingFull.getMunicipalityDistrict());
		listingPO.setProvince(listingFull.getProvince());
		listingPO.setSellerPropertyInfoStatement(listingFull.getSellerPropertyInfoStatement());
		listingPO.setPixUpdtedDt(listingFull.getPixUpdatedDate());
		listingPO.setStatus(listingFull.getStatus());
		listingPO.setListBrokerage(listingFull.getListBrokerage());
		
		return listingPO;
		
	}
	
	private FH_ListingAddrPO saveIntoAddressTable(FullResListing listingFull)
	{

		FH_ListingAddrPO addrPO = null;
		
		//fh_listing
		//System.out.println(listingFull.getMls() + " into fr_address table:");
		//fh_address
		addrPO = new FH_ListingAddrPO();
		addrPO.setMLS(listingFull.getMls());
		addrPO.setArea(listingFull.getArea());
		addrPO.setMap(listingFull.getMap());
		addrPO.setMapColumn(listingFull.getMapColumn());
		addrPO.setMapRow(listingFull.getMapRow());
		addrPO.setStreet(listingFull.getStreet());
		addrPO.setStreetDirection(listingFull.getStreetDirection());
		addrPO.setStreetAbbreviation(listingFull.getStreetAbbreviation());
		addrPO.setAreaCode(listingFull.getAreaCode());
		addrPO.setMunicipalityCode(listingFull.getMunicipalityCode());
		addrPO.setCommunityCode(listingFull.getCommunityCode());
		addrPO.setMunicipalityDistrict(listingFull.getMunicipalityDistrict());
		addrPO.setMunicipality(listingFull.getMunicipality());
		addrPO.setCommunity(listingFull.getCommunity());
		
		return addrPO;
		
	}
	
	private FH_ListingExtraPO saveIntoExtraTable(FullResListing listingFull)
	{

		
		FH_ListingExtraPO extraPO = null;
		System.out.println(listingFull.getMls() + " into fr_property_extra table:");

		//fh extra
		extraPO = new FH_ListingExtraPO();
		extraPO.setMLS(listingFull.getMls());
		extraPO.setExtras(listingFull.getExtras());
		extraPO.setRemarksForClients(listingFull.getRemarksForClients().getBytes());				
		

		return extraPO	;
	}
	
	private FH_PropertyPO saveIntoPropertyTable(FullResListing listingFull)
	{

		FH_PropertyPO ppyPO = null;
		//System.out.println(listingFull.getMls() + " into fr_property table:");
		//fh property
		ppyPO = new FH_PropertyPO();
		ppyPO.setMLS(listingFull.getMls());
		ppyPO.setTypeOwn1Out(listingFull.getTypeOwn1out());
		ppyPO.setStyle(listingFull.getStyle());
		ppyPO.setFrontingOnNSEW(listingFull.getFontingOnNSEW());
		ppyPO.setAcreage(listingFull.getAcreage());
		ppyPO.setRooms(listingFull.getRooms());
		ppyPO.setRoomsPlus(listingFull.getRoomsPlus());
		ppyPO.setBedrooms(listingFull.getBedrooms());
		ppyPO.setBedroomsPlus(listingFull.getBedroomPlus());
		ppyPO.setWashrooms(listingFull.getWashrooms());
		
		ppyPO.setLotFront(listingFull.getLotFront());
		ppyPO.setLotDepth(listingFull.getLotDepth());
		ppyPO.setLotIrregularities(listingFull.getLotIrregularities());
		ppyPO.setDirectionsCrossStreets(listingFull.getDirectionsCrossStreets());
		
		ppyPO.setWashroomsType1Pcs(listingFull.getWashroomsType1Pcs());
		ppyPO.setWashroomsType2Pcs(listingFull.getWashroomsType2Pcs());
		ppyPO.setWashroomsType3Pcs(listingFull.getWashroomsType3Pcs());
		ppyPO.setWashroomsType4Pcs(listingFull.getWashroomsType4Pcs());
		ppyPO.setWashroomsType1(listingFull.getWashroomsType1());
		ppyPO.setWashroomsType2(listingFull.getWashroomsType2());
		ppyPO.setWashroomsType3(listingFull.getWashroomsType3());
		ppyPO.setWashroomsType4(listingFull.getWashroomsType4());
		ppyPO.setWashroomsType1Level(listingFull.getWashroomsType1Level());
		ppyPO.setWashroomsType2Level(listingFull.getWashroomsType2Level());
		ppyPO.setWashroomsType3Level(listingFull.getWashroomsType3Level());
		ppyPO.setWashroomsType4Level(listingFull.getWashroomsType4Level());
		
		return ppyPO;
		
	}
	
	private FH_PropertyDetailsPO saveIntoPropertyDetailsTable(FullResListing listingFull)
	{

		FH_PropertyDetailsPO ppyDtlPO = null;
		//System.out.println(listingFull.getMls() + " into fr_property_details table:");

		//fh_property_details
		ppyDtlPO = new FH_PropertyDetailsPO();
		ppyDtlPO.setMLS(listingFull.getMls());
		ppyDtlPO.setKitchens(listingFull.getKitchens());
		ppyDtlPO.setFamilyRoom(listingFull.getFamilyRoom());
		ppyDtlPO.setBasement1(listingFull.getBasement1());
		ppyDtlPO.setBasement2(listingFull.getBasement2());
		ppyDtlPO.setExterior1(listingFull.getExterior1());
		ppyDtlPO.setExterior2(listingFull.getExterior2());
		ppyDtlPO.setFireplaceStove(listingFull.getFireplaceStove());
		ppyDtlPO.setHeatType(listingFull.getHeatType());
		ppyDtlPO.setHeatSource(listingFull.getHeatSource());
		ppyDtlPO.setAirConditioning(listingFull.getAirConditioning());
		ppyDtlPO.setCentralVac(listingFull.getCentralVac());
		ppyDtlPO.setApproxAge(listingFull.getApproxAge());
		ppyDtlPO.setApproxSquareFootage(listingFull.getApproxSquareFootage());
		ppyDtlPO.setAssessment(listingFull.getAssessment());
		ppyDtlPO.setElevator(listingFull.getElevator());
		ppyDtlPO.setLaundryLevel(listingFull.getLaundryLevel());
		ppyDtlPO.setDrive(listingFull.getDrive());
		ppyDtlPO.setGarageType(listingFull.getGarageType());
		ppyDtlPO.setGarageSpaces(listingFull.getGarageSpaces());
		
		ppyDtlPO.setParkingSpaces(listingFull.getParkingSpaces());
		ppyDtlPO.setPool(listingFull.getPool());
		ppyDtlPO.setUffi(listingFull.getUffi());
		ppyDtlPO.setPropertyFeatures1(listingFull.getPropertyFeatures1());
		ppyDtlPO.setPropertyFeatures2(listingFull.getPropertyFeatures2());
		ppyDtlPO.setPropertyFeatures3(listingFull.getPropertyFeatures3());
		ppyDtlPO.setPropertyFeatures4(listingFull.getPropertyFeatures4());
		ppyDtlPO.setPropertyFeatures5(listingFull.getPropertyFeatures5());
		ppyDtlPO.setPropertyFeatures6(listingFull.getPropertyFeatures6());
		
		ppyDtlPO.setZoning(listingFull.getZoning());
		ppyDtlPO.setCableTVIncluded(listingFull.getCableTVIncluded());
		ppyDtlPO.setHydroIncluded(listingFull.getHydroIncluded());
		ppyDtlPO.setUtilitiesCable(listingFull.getUtilitiesCable());
		ppyDtlPO.setUtilitiesTelephone(listingFull.getUtilitiesTelephone());
		
		ppyDtlPO.setWater(listingFull.getWater());
		ppyDtlPO.setWaterIncluded(listingFull.getWaterIncluded());
		ppyDtlPO.setWaterSupplyTypes(listingFull.getWaterSupplyTypes());
		ppyDtlPO.setSewers(listingFull.getSewers());
		ppyDtlPO.setSpecialDesignation1(listingFull.getSpecialDesignation1());
		ppyDtlPO.setSpecialDesignation2(listingFull.getSpecialDesignation2());
		ppyDtlPO.setSpecialDesignation3(listingFull.getSpecialDesignation3());
		ppyDtlPO.setSpecialDesignation4(listingFull.getSpecialDesignation4());
		ppyDtlPO.setSpecialDesignation5(listingFull.getSpecialDesignation5());
		ppyDtlPO.setSpecialDesignation6(listingFull.getSpecialDesignation6());
		
		ppyDtlPO.setFarmAgriculture(listingFull.getFarmAgriculture());
		ppyDtlPO.setWaterfront(listingFull.getWaterfront());
		ppyDtlPO.setRetirement(listingFull.getRetirement());
		ppyDtlPO.setOtherStructures1(listingFull.getOtherStructures1());
		ppyDtlPO.setOtherStructures2(listingFull.getOtherStructures2());

		return ppyDtlPO;
		
	}
	
	private FH_PropertyRoomsPO saveIntoPropertyRoomsTable(FullResListing listingFull)
	{

		
		FH_PropertyRoomsPO ppyRoomPO = null;
		//System.out.println(listingFull.getMls() + " into fr_property_rooms table:");

		//fh property rooms
				//fh property rooms
				ppyRoomPO = new FH_PropertyRoomsPO();
				ppyRoomPO.setMLS(listingFull.getMls());
				ppyRoomPO.setLevel1(listingFull.getLevel_1());
				ppyRoomPO.setLevel2(listingFull.getLevel_2());
				ppyRoomPO.setLevel3(listingFull.getLevel_3());
				ppyRoomPO.setLevel4(listingFull.getLevel_4());
				ppyRoomPO.setLevel5(listingFull.getLevel_5());
				ppyRoomPO.setLevel6(listingFull.getLevel_6());
				ppyRoomPO.setLevel7(listingFull.getLevel_7());
				ppyRoomPO.setLevel8(listingFull.getLevel_8());
				ppyRoomPO.setLevel9(listingFull.getLevel_9());
				ppyRoomPO.setLevel10(listingFull.getLevel_10());
				ppyRoomPO.setLevel11(listingFull.getLevel_11());
				ppyRoomPO.setLevel12(listingFull.getLevel_12());

				ppyRoomPO.setRoom1(listingFull.getRoom1());
				ppyRoomPO.setRoom1Length(listingFull.getRoom1Length());
				ppyRoomPO.setRoom1Width(listingFull.getRoom1Width());
				ppyRoomPO.setRoom1Desc1(listingFull.getRoom1Desc1());
				ppyRoomPO.setRoom1Desc2(listingFull.getRoom1Desc2());
				ppyRoomPO.setRoom1Desc3(listingFull.getRoom1Desc3());
			
				ppyRoomPO.setRoom2(listingFull.getRoom2());
				ppyRoomPO.setRoom2Length(listingFull.getRoom2Length());
				ppyRoomPO.setRoom2Width(listingFull.getRoom2Width());
				ppyRoomPO.setRoom2Desc1(listingFull.getRoom2Desc1());
				ppyRoomPO.setRoom2Desc2(listingFull.getRoom2Desc2());
				ppyRoomPO.setRoom2Desc3(listingFull.getRoom2Desc3());

				ppyRoomPO.setRoom3(listingFull.getRoom3());
				ppyRoomPO.setRoom3Length(listingFull.getRoom3Length());
				ppyRoomPO.setRoom3Width(listingFull.getRoom3Width());
				ppyRoomPO.setRoom3Desc1(listingFull.getRoom3Desc1());
				ppyRoomPO.setRoom3Desc2(listingFull.getRoom3Desc2());
				ppyRoomPO.setRoom3Desc3(listingFull.getRoom3Desc3());

				ppyRoomPO.setRoom4(listingFull.getRoom4());
				ppyRoomPO.setRoom4Length(listingFull.getRoom4Length());
				ppyRoomPO.setRoom4Width(listingFull.getRoom4Width());
				ppyRoomPO.setRoom4Desc1(listingFull.getRoom4Desc1());
				ppyRoomPO.setRoom4Desc2(listingFull.getRoom4Desc2());
				ppyRoomPO.setRoom4Desc3(listingFull.getRoom4Desc3());
				
				ppyRoomPO.setRoom5(listingFull.getRoom5());
				ppyRoomPO.setRoom5Length(listingFull.getRoom5Length());
				ppyRoomPO.setRoom5Width(listingFull.getRoom5Width());
				ppyRoomPO.setRoom5Desc1(listingFull.getRoom5Desc1());
				ppyRoomPO.setRoom5Desc2(listingFull.getRoom5Desc2());
				ppyRoomPO.setRoom5Desc3(listingFull.getRoom5Desc3());

				ppyRoomPO.setRoom6(listingFull.getRoom6());
				ppyRoomPO.setRoom6Length(listingFull.getRoom6Length());
				ppyRoomPO.setRoom6Width(listingFull.getRoom6Width());
				ppyRoomPO.setRoom6Desc1(listingFull.getRoom6Desc1());
				ppyRoomPO.setRoom6Desc2(listingFull.getRoom6Desc2());
				ppyRoomPO.setRoom6Desc3(listingFull.getRoom6Desc3());

				ppyRoomPO.setRoom7(listingFull.getRoom7());
				ppyRoomPO.setRoom7Length(listingFull.getRoom7Length());
				ppyRoomPO.setRoom7Width(listingFull.getRoom7Width());
				ppyRoomPO.setRoom7Desc1(listingFull.getRoom7Desc1());
				ppyRoomPO.setRoom7Desc2(listingFull.getRoom7Desc2());
				ppyRoomPO.setRoom7Desc3(listingFull.getRoom7Desc3());

				ppyRoomPO.setRoom8(listingFull.getRoom8());
				ppyRoomPO.setRoom8Length(listingFull.getRoom8Length());
				ppyRoomPO.setRoom8Width(listingFull.getRoom8Width());
				ppyRoomPO.setRoom8Desc1(listingFull.getRoom8Desc1());
				ppyRoomPO.setRoom8Desc2(listingFull.getRoom8Desc2());
				ppyRoomPO.setRoom8Desc3(listingFull.getRoom8Desc3());
				
				ppyRoomPO.setRoom9(listingFull.getRoom9());
				ppyRoomPO.setRoom9Length(listingFull.getRoom9Length());
				ppyRoomPO.setRoom9Width(listingFull.getRoom9Width());
				ppyRoomPO.setRoom9Desc1(listingFull.getRoom9Desc1());
				ppyRoomPO.setRoom9Desc2(listingFull.getRoom9Desc2());
				ppyRoomPO.setRoom9Desc3(listingFull.getRoom9Desc3());
				
				ppyRoomPO.setRoom10(listingFull.getRoom10());
				ppyRoomPO.setRoom10Length(listingFull.getRoom10Length());
				ppyRoomPO.setRoom10Width(listingFull.getRoom10Width());
				ppyRoomPO.setRoom10Desc1(listingFull.getRoom10Desc1());
				ppyRoomPO.setRoom10Desc2(listingFull.getRoom10Desc2());
				ppyRoomPO.setRoom10Desc3(listingFull.getRoom10Desc3());
				
				ppyRoomPO.setRoom11(listingFull.getRoom11());
				ppyRoomPO.setRoom11Length(listingFull.getRoom11Length());
				ppyRoomPO.setRoom11Width(listingFull.getRoom11Width());
				ppyRoomPO.setRoom11Desc1(listingFull.getRoom11Desc1());
				ppyRoomPO.setRoom11Desc2(listingFull.getRoom11Desc2());
				ppyRoomPO.setRoom11Desc3(listingFull.getRoom11Desc3());

				ppyRoomPO.setRoom12(listingFull.getRoom12());
				ppyRoomPO.setRoom12Length(listingFull.getRoom12Length());
				ppyRoomPO.setRoom12Width(listingFull.getRoom12Width());
				ppyRoomPO.setRoom12Desc1(listingFull.getRoom12Desc1());
				ppyRoomPO.setRoom12Desc2(listingFull.getRoom12Desc2());
				ppyRoomPO.setRoom12Desc3(listingFull.getRoom12Desc3());


		return ppyRoomPO;
		
	}
	
}
