Vue.component("tabela-js",{
    data: function()
    {
        return {
            items: [],
            fields:[
                {key: 'id', sortable: true},
                {key: 'name', sortable: true}
            ],
            selectedAmenity: {},
            deletedAmenity: {},
            componentKey:0
        }
    },
    template:`
    <div id>
        <b-table selectable
         @row-selected="selectAmenity" 
         striped bordered 
         hover 
         :items="items" 
         :fields="fields"
         select-mode="single"
         ></b-table>
    </div>
    ` 
    ,
    mounted()
    {
        let jwt = window.localStorage.getItem('jwt');
        if(!jwt)
            jwt='';
        console.log(jwt);
        

        axios
        .get('rest/vazduhbnb/amenities',{
            headers:{
                'Authorization': 'Bearer ' + jwt
            }
        })
        .then(response =>{
            this.items = response.data;  
        })
        .catch(function(error){
            console.log(error.response)
            switch(error.response.status)
            {
                case 500:
                    pushErrorNotification('Interval server error','Please try again later')
                    break;
                case 403:
                    pushErrorNotification("Forbiden","You must be logged in as admin");
                  break;
            }
        });

        /*this.$root.$on('deleteAmenity', (deletedAmenity)=>{
            let i = items.indexOf(deletedAmenity);

            if(i > -1)
            {
                items.splice(i, 1);
            }
        });*/
    },
    methods:
    {
        sendToSibling: function(){
            this.$root.$emit('messageToSibling', this.selectedAmenity);
        },
        selectAmenity: function(amenity)
        {
            if(amenity.length != 0)
            {
                this.selectedAmenity = amenity[0];
                this.$root.$emit('selectedAmenity', this.selectedAmenity);
            }
            else
            {
                this.$root.$emit('selectedAmenity', this.selectedAmenity);
            }  
        },

    },

});

Vue.component("name-form",{
    data: function(){
        return{
            mode: "BROWSE",
            selectedAmenity: {} ,
            seachField: ""
        }
    },
    template:`
    <div>
        <b-form>
        <b-form-group label="Amenity name:">
        </b-form-group>
        <b-form-input 
        v-model="selectedAmenity.name" 
        :formatter="formatter"
        placeholder="Selected amenity's name will be written here"></b-form-input>

        <br>
        <b-button 
        @click="editName" 
        class="mr-1" 
        pill
        variant="success">Edit</b-button>

        <b-button 
        @click="deleteAmenity" 
        class="mr-1" 
        pill
        variant="danger">Delete</b-button>
        </b-form>
    </div>
    `,
    mounted(){
        this.$root.$on('selectedAmenity', (selectedAmenity)=>{
            this.selectedAmenity = selectedAmenity;
            console.log(this.selectedAmenity.name);
        });
    },
    methods:
    {
        formatter: function(value)
        {
            if(value.length > 100)
                return value.substring(0,100);
            return value;
        },
        editName: function()
        {
            let updatedAmenity={
                id: this.selectedAmenity.id,
                name: this.selectedAmenity.name,
                deleted: this.selectedAmenity.deleted
            }

            if(updatedAmenity.id == null)
            {
                pushErrorNotification('Error!','You must select amenity!');
                return;
            }

            axios
                .put('rest/vazduhbnb/updateAmenityName', updatedAmenity)
                .then(response =>{
                    console.log('amenity updated successfully');
                    pushSuccessNotification('Success!','Amenity updated successfully!');
                });
        },
        deleteAmenity: function()
        {
            if(this.selectedAmenity.id == null)
            {
                pushErrorNotification('Error!','You must select amenity first!');
                return;
            }

            let deletedAmenity={
                id: this.selectedAmenity.id,
                name: this.selectedAmenity.name,
                deleted: this.selectedAmenity.deleted
            }

            if(deletedAmenity.deleted == true)
            {
                pushErrorNotification('Error!','You must select not deleted amenity!');
                return;
            }

            axios
                .put('rest/vazduhbnb/deleteAmenity', deletedAmenity)
                .then(response =>{
                    console.log('amenity deleted successfully');
                    pushSuccessNotification('Success!','Amenity deleted successfully!');
                });
            window.location.reload();

            this.$root.$emit('deleteAmenity', deletedAmenity);
        }
            

            
    }
})

Vue.component("add-form",{
    data: function(){
        return{
            name:''
        }
    },
    template:`
    <div>
        <b-form>
        <b-form-group label="Add new amenity:">
        </b-form-group>
        <b-form-input
        :formatter="formatter"
        v-model="name"
        placeholder="Enter name"></b-form-input>
        <br>
        <b-button @click="addNew" 
        class="mr-1" 
        pill
        variant="success">Add</b-button>
        </b-form>
    </div>
    `,
    methods:
    {
        formatter: function(value)
        {
            if(value.length > 100)
                return value.substring(0,100);
            return value;
        },
        addNew: function()
        {
            if(this.name != '')
            {
                axios
                    .post('rest/vazduhbnb/addNewAmenity', this.name)
                    .then(response =>{
                        pushSuccessNotification('Success!', 'You added new amenity to the list!');
                        window.location.reload();
                    })
            }
        }
    }
});

var app = new Vue({
    el: "#amenities"
})