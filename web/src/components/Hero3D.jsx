import React, { useRef, useMemo } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { Float, Sphere, Text } from '@react-three/drei';
import * as THREE from 'three';

// -------------------------------------------------------------
// Interactive 3D Cyber Network with Intruder & Protective Shield
// -------------------------------------------------------------
function NetworkGraph() {
  const groupRef = useRef();
  const redNodeRef = useRef();
  const shieldRef = useRef();

  // Generate 45 stable node coordinates
  const { nodePositions, lineGeometry } = useMemo(() => {
    const count = 45;
    const positions = [];
    for (let i = 0; i < count; i++) {
      const x = (Math.random() - 0.5) * 14;
      const y = (Math.random() - 0.5) * 8;
      const z = (Math.random() - 0.5) * 6;
      positions.push(new THREE.Vector3(x, y, z));
    }

    // Connect close neighbors
    const linePoints = [];
    for (let i = 0; i < count; i++) {
      for (let j = i + 1; j < count; j++) {
        const dist = positions[i].distanceTo(positions[j]);
        if (dist < 3.2) {
          linePoints.push(positions[i].x, positions[i].y, positions[i].z);
          linePoints.push(positions[j].x, positions[j].y, positions[j].z);
        }
      }
    }

    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.Float32BufferAttribute(linePoints, 3));

    return { nodePositions: positions, lineGeometry: geometry };
  }, []);

  useFrame((state) => {
    const time = state.clock.getElapsedTime();

    // Subtle ambient rotation
    if (groupRef.current) {
      groupRef.current.rotation.y = Math.sin(time * 0.15) * 0.1;
      groupRef.current.rotation.x = Math.cos(time * 0.1) * 0.05;
    }

    // Malicious Red Node trajectory (attempts to strike target at [0, 0, 0])
    if (redNodeRef.current) {
      const cycle = (time * 0.7) % 5; // 0 to 5 seconds loop
      if (cycle < 3.2) {
        // Approaching target
        const t = cycle / 3.2;
        redNodeRef.current.position.set(
          THREE.MathUtils.lerp(-6, -0.6, t),
          THREE.MathUtils.lerp(3, 0.2, t),
          THREE.MathUtils.lerp(2, 0, t)
        );
        redNodeRef.current.scale.setScalar(1 + Math.sin(time * 8) * 0.2);
      } else {
        // Intercepted & Deflected
        const tDeflect = (cycle - 3.2) / 1.8;
        redNodeRef.current.position.set(
          -0.6 - tDeflect * 4,
          0.2 + tDeflect * 2,
          -tDeflect * 3
        );
      }
    }

    // Protective Shield expansion upon interception
    if (shieldRef.current) {
      const cycle = (time * 0.7) % 5;
      if (cycle >= 2.8 && cycle <= 4.0) {
        shieldRef.current.scale.setScalar(1.4 + Math.sin(time * 12) * 0.15);
        shieldRef.current.material.opacity = 0.85;
      } else {
        shieldRef.current.scale.setScalar(1.0);
        shieldRef.current.material.opacity = 0.35;
      }
      shieldRef.current.rotation.y += 0.02;
    }
  });

  return (
    <group ref={groupRef}>
      {/* Network Edges / Laser Lines */}
      <lineSegments geometry={lineGeometry}>
        <lineBasicMaterial color="#06b6d4" transparent opacity={0.22} />
      </lineSegments>

      {/* Network Nodes */}
      {nodePositions.map((pos, idx) => (
        <mesh key={idx} position={pos}>
          <sphereGeometry args={[0.07, 12, 12]} />
          <meshBasicMaterial color={idx % 4 === 0 ? '#10b981' : '#38bdf8'} />
        </mesh>
      ))}

      {/* Central Core API Endpoint */}
      <mesh position={[0, 0, 0]}>
        <sphereGeometry args={[0.3, 24, 24]} />
        <meshStandardMaterial
          color="#10b981"
          emissive="#10b981"
          emissiveIntensity={1.8}
          roughness={0.2}
        />
      </mesh>

      {/* 🛡️ Emerald Protective Shield (Defending Scanner) */}
      <mesh ref={shieldRef} position={[0, 0, 0]}>
        <icosahedronGeometry args={[0.75, 2]} />
        <meshStandardMaterial
          color="#10b981"
          wireframe
          transparent
          opacity={0.4}
          emissive="#34d399"
          emissiveIntensity={1.2}
        />
      </mesh>

      {/* 🚨 Malicious Intruder Node */}
      <mesh ref={redNodeRef} position={[-6, 3, 2]}>
        <sphereGeometry args={[0.18, 16, 16]} />
        <meshStandardMaterial
          color="#f43f5e"
          emissive="#f43f5e"
          emissiveIntensity={2.5}
          roughness={0.1}
        />
      </mesh>
    </group>
  );
}

export default function Hero3D() {
  return (
    <div className="absolute inset-0 pointer-events-none z-0 overflow-hidden">
      <Canvas
        camera={{ position: [0, 0, 7.5], fov: 48 }}
        gl={{ antialias: true, alpha: true }}
        dpr={[1, 1.5]}
      >
        <ambientLight intensity={0.6} />
        <pointLight position={[5, 6, 4]} intensity={2.2} color="#10b981" />
        <pointLight position={[-6, -4, -2]} intensity={1.8} color="#6366f1" />
        <Float speed={1.4} rotationIntensity={0.2} floatIntensity={0.4}>
          <NetworkGraph />
        </Float>
      </Canvas>

      {/* Radial Vignette Mask */}
      <div className="absolute inset-0 bg-gradient-to-t from-[#07090e] via-transparent to-[#07090e] opacity-80" />
      <div className="absolute inset-0 bg-gradient-to-r from-[#07090e] via-transparent to-[#07090e] opacity-70" />
    </div>
  );
}
