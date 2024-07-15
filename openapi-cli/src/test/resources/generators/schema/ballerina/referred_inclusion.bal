import ballerina/http;

public type SimpleType int;

public type Pets Pet[];

public type TestPet Pet;

public type TestPetAccepted record {|
    *http:Accepted;
    TestPet body;
|};

public type TestDog Dog;

public type ReferredSimpleType SimpleType;

public type Error record {
    int code;
    string message;
};

public type Dog record {
    *Pet;
    boolean bark?;
};

public type Pet record {
    int id;
    string name;
    string tag?;
    string 'type?;
};
