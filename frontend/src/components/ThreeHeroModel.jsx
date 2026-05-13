import { useEffect, useRef } from "react";

import * as THREE from "three";

import { GLTFLoader } from "three/examples/jsm/loaders/GLTFLoader";

export default function ThreeHeroModel() {

  const mountRef = useRef();

  useEffect(() => {

  const scene = new THREE.Scene();

  const camera = new THREE.PerspectiveCamera(

  75,

  window.innerWidth / (window.innerHeight * 0.7),

  0.1,

  1000,

  );

  const renderer = new THREE.WebGLRenderer({ alpha: true });

  renderer.setSize(window.innerWidth, window.innerHeight * 0.7);

  mountRef.current.appendChild(renderer.domElement);

  const light = new THREE.DirectionalLight(0xffffff, 2);

  light.position.set(5, 5, 5);

  scene.add(light);

  //camera.position.z = 5;

  camera.position.set(0, 0, 6.5);

  const loader = new GLTFLoader();

  loader.load("/models/evtol.glb", (gltf) => {

  const model = gltf.scene;

  const rotors = [];

  //propellers rotation-storing each propeller name in an array

  // model.traverse((child) => {

  // if (child.name.includes("Rotor")) {

  // rotors.push(child);

  // }

  // });

  model.scale.set(0.1, 0.1, 0.1);

  model.position.y = 1.5;

  scene.add(model);

  let time = 0;

  function animate() {

  requestAnimationFrame(animate);

  time += 0.02;

  model.position.y = Math.sin(time) * 0.5;

  model.rotation.y += 0.009;

  model.rotation.z = Math.sin(time) * 0.05;

  //Rotating the propellers-2 rotors spin clockwise,2 spin counterclockwise

  // rotors.forEach((rotor) => {

  // if (rotor.name === "Rotor_FL" || rotor.name === "Rotor_BR") {

  // rotor.rotation.y += 0.2;

  // } else {

  // rotor.rotation.y -= 0.2;

  // }

  // });

  renderer.render(scene, camera);

  }

  animate();

  });

  }, []);

  return (

  <div

  ref={mountRef}

  style={{

  position: "absolute",

  top: 0,

  left: 0,

  width: "100%",

  height: "100%",

  zIndex: 1,

  }}

  />

  );

}