package hiberspring.service.impl;

import hiberspring.common.GlobalConstants;
import hiberspring.domain.dtos.EmployeeSeedRootDto;
import hiberspring.domain.entities.Branch;
import hiberspring.domain.entities.Employee;
import hiberspring.domain.entities.EmployeeCard;
import hiberspring.repository.EmployeeRepository;
import hiberspring.service.BranchService;
import hiberspring.service.EmployeeCardService;
import hiberspring.service.EmployeeService;
import hiberspring.util.ValidationUtil;
import hiberspring.util.XmlParser;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImpl implements EmployeeService {


    private final EmployeeRepository employeeRepository;
    private final ModelMapper modelMapper;
    private final ValidationUtil validationUtil;
    private final XmlParser xmlParser;
    private final BranchService brnchService;
    private final EmployeeCardService employeeCardService;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository, ModelMapper modelMapper, ValidationUtil validationUtil, XmlParser xmlParser, BranchService brnchService, EmployeeCardService employeeCardService) {
        this.employeeRepository = employeeRepository;
        this.modelMapper = modelMapper;
        this.validationUtil = validationUtil;
        this.xmlParser = xmlParser;
        this.brnchService = brnchService;
        this.employeeCardService = employeeCardService;
    }

    @Override
    public Boolean employeesAreImported() {
        return this.employeeRepository.count() > 0;
    }

    @Override
    public String readEmployeesXmlFile() throws IOException {
        return Files.readString(Path.of(GlobalConstants.EMPLOYEES_FILE_PATH));
    }

    @Override
    public String importEmployees() throws JAXBException, FileNotFoundException {

        StringBuilder resultInfo = new StringBuilder();

        EmployeeSeedRootDto employeeSeedRootDto = this.xmlParser
                .parseXml(EmployeeSeedRootDto.class, GlobalConstants.EMPLOYEES_FILE_PATH);

        employeeSeedRootDto.getEmployees()
                .forEach(employeeSeedDto -> {
                    if(this.validationUtil.isValid(employeeSeedDto)){

                        if(this.employeeRepository.findByFirstNameAndLastNameAndPosition(employeeSeedDto.getFirstName(), employeeSeedDto.getLastName(), employeeSeedDto.getPosition())== null){


                            Employee employee = this.modelMapper
                                    .map(employeeSeedDto, Employee.class);
                            resultInfo.append(String.format(GlobalConstants.SUCCESSFUL_IMPORT_MESSAGE, employee.getClass().getSimpleName(),employee.getFirstName()));
                            Branch branch = this.brnchService.getBranchByName(employeeSeedDto.getBranch());
                            EmployeeCard employeeCard = this.employeeCardService.getEmployeeCardByNumber(employeeSeedDto.getCard());
                            employee.setBranch(branch);
                            employee.setCard(employeeCard);
                            this.employeeRepository.saveAndFlush(employee);
                        }else{
                            resultInfo.append(GlobalConstants.IN_DB_MESSAGE);
                        }
                    }else{
                        resultInfo.append(GlobalConstants.INCORRECT_DATA_MESSAGE);
                    }
                    resultInfo.append(System.lineSeparator());
                });
        return resultInfo.toString();
    }

    @Override
    public String exportProductiveEmployees() {

      return this.employeeRepository
              .findALlByBranchWithMoreThanOneProduct()
              .stream()
              .map(e ->{
               return String.format("%nName: %s %s\n" +
                       "Position: %s\n" +
                       "Card Number: %s\n", e.getFirstName(), e.getLastName(), e.getPosition(), e.getCard().getNumber());
              }).collect(Collectors.joining("-------------------------"));
    }
}
