var app = new Vue({
    el: "#app",
    data: {
        name: '',
        numberOfRooms: 1,
        numberOfGuests: 1,
        pricePerNightString: '',
        pricePerNight : 0,
        apartmentType: 'fullApartment',
        latitude: '',
        longitude: '',
        street: '',
        houseNumber: '',
        city: '',
        postalCode: '',
        position: {lat:0, lng:0},

        checkInHour: 14,
        checkOutHour: 10,
        
        pictures: [],
        amenities: [],
        apartmentTypeEnum: [
            { value: 'fullApartment', text: 'Full Apartment' },
            { value: 'room', text: 'Room' }
        ],
        bookingDates : [],

        picturesErrorMessage: '',
        priceErrorMessage: '',
        positionErrorMessage: '',
        nameErrorMessage:'',
        positionValid: false,
        priceValid : false,
        picturesValid: false,
        nameValid:false
    },
    mounted() {
    },
    methods: {
        successMessage: function(){
            this.$bvModal.msgBoxOk('Your apartment was created successfully', {
                title: 'Apartment Created',
                size: 'sm',
                buttonSize: 'sm',
                okVariant: 'success',
                headerClass: 'p-2 border-bottom-0',
                footerClass: 'p-2 border-top-0',
                centered: true
            })
            .then(value => {
                window.location.href = "index.html";
            })
            .catch(err => {
                // An error occurred
            });
        },
        create : function(){
            if(this.isDataValid){
                let data = {
                    name: this.name,
                    numberOfRooms : this.numberOfRooms,
                    numberOfGuests : this.numberOfGuests,
                    pricePerNight : this.pricePerNight,
                    apartmentType : this.apartmentType,
                    latitude : this.latitude,
                    longitude : this.longitude,
                    street : this.street,
                    houseNumber : this.houseNumber,
                    city : this.city,
                    postalCode : this.postalCode,
                    pictures : this.pictures,
                    amenities : this.amenities,
                    bookingDates : this.bookingDates.map(d => d.getTime()),
                    checkInHour: this.checkInHour,
                    checkOutHour: this.checkOutHour
                }

                let jwt = window.localStorage.getItem('jwt');
                if (!jwt)
                    jwt = '';

                const vm = this;

                axios
                    .post('rest/vazduhbnb/apartment', data, {headers:{'Authorization': 'Bearer ' + jwt}})
                    .then(function(response){
                        vm.successMessage();
                    })
                    .catch(function(error){
                        let response = error.response;
                        switch(response.status){
                            case 400: pushErrorNotification("Error", response.data.message); break;
                            case 401: alert(unauthorizedErrorMessage); signOut(); break;
                            case 403: alert(forbiddenErrorMessage); signOut(); break;
                            case 500: pushInternalServerError(); break;
                        }
                    });
            }
        },
        
    },
    watch:{
        position: function(newPosition, oldPosition){
            this.latitude = newPosition.lat;
            this.longitude = newPosition.lng;

            const vm = this;

            axios
                .get("https://us1.locationiq.com/v1/reverse.php?key=d9ed4cd3090a09&lat=" + this.latitude + "&lon=" + this.longitude + "&format=json")
                .then(function(response){
                    let address = response.data.address;
                    vm.city = transliterate(address.city_district ? address.city_district : address.city);
                    vm.postalCode = transliterate(address.postcode);
                    vm.street = transliterate(address.road);
                    vm.houseNumber = transliterate(address.house_number);
                })
                .catch(function(error){
                    console.log(error);
                });

        },
        apartmentType : function(newType, oldType){
            if(newType == 'room'){
                this.numberOfRooms = 1;
            }
        },
        pictures : function(newPics, oldPics){
            if(newPics)
                this.picturesValid = true;
        }
    },
    computed:{
        priceValidation(){
            this.pricePerNight = parseFloat(this.pricePerNightString);

            if(!this.pricePerNight){
                this.priceErrorMessage = "Please enter a number.";
                this.priceValid = false;
                return false;
            }

            if(this.pricePerNight <= 0){
                this.priceValid = false;
                this.priceErrorMessage = "Please enter a positive number.";
                return false;
            }
            
            this.priceValid = true;
            return true;
        },
        positionValidation(){
            if(this.latitude && this.longitude){
                this.positionErrorMessage = '';
                this.positionValid = true;
                return true;
            }
            else{
                this.positionErrorMessage = "Please select a location by clicking on the map.";
                this.positionValid = false;
                return false;
            }
        },
        nameValidation(){
            if(this.name.trim() == ''){
                this.nameErrorMessage = 'Please enter apartment name';
                this.nameValid = false;
                return false;
            }
            else{
                this.nameValid = true;
                return true;
            }
        },
        isDataValid : function(){
            return this.positionValid && this.priceValid && this.picturesValid && this.nameValid;
        }
    }
});